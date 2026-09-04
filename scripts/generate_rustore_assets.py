#!/usr/bin/env python3
"""Generate RuStore icon and 9:16 listing screenshots from reviewed app assets."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SOURCE_SCREENSHOTS = (
    ROOT
    / "app/src/screenshotTestDebug/reference/com/worktime/app/ui/screenshot/WorkTimeScreenshotTestKt"
)
OUTPUT = ROOT / "docs/rustore/assets"
OUTPUT.mkdir(parents=True, exist_ok=True)

WIDTH = 1080
HEIGHT = 1920
FONT = Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")
BOLD_FONT = Path("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")

SCREENSHOTS = {
    "calendar_light": SOURCE_SCREENSHOTS
    / "CalendarPopulatedLightScreenshot_Calendar populated light_d57e6e9a_0.png",
    "calendar_dark": SOURCE_SCREENSHOTS
    / "CalendarPopulatedDarkScreenshot_Calendar populated dark_1552a2cd_0.png",
    "year_light": SOURCE_SCREENSHOTS
    / "YearSummaryPopulatedScreenshot_Year summary populated_b8aa34c2_0.png",
    "year_dark": SOURCE_SCREENSHOTS
    / "YearSummaryEmptyDarkScreenshot_Year summary empty dark_df63d121_0.png",
    "year_large": SOURCE_SCREENSHOTS
    / "YearSummaryLargeFontScreenshot_Year summary large font_6a4e2587_0.png",
}


def fitted_font(draw: ImageDraw.ImageDraw, text: str, max_width: int, start: int, minimum: int, *, bold: bool) -> ImageFont.FreeTypeFont:
    path = BOLD_FONT if bold else FONT
    for size in range(start, minimum - 1, -2):
        font = ImageFont.truetype(str(path), size)
        box = draw.textbbox((0, 0), text, font=font)
        if box[2] - box[0] <= max_width:
            return font
    return ImageFont.truetype(str(path), minimum)


def rounded_mask(size: tuple[int, int], radius: int) -> Image.Image:
    mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, size[0], size[1]), radius=radius, fill=255)
    return mask


def place_screen(
    canvas: Image.Image,
    screen: Image.Image,
    *,
    top: int,
    bottom: int = 55,
    max_width: int = 790,
    radius: int = 34,
) -> None:
    available_height = HEIGHT - top - bottom
    scale = min(max_width / screen.width, available_height / screen.height)
    new_size = (int(screen.width * scale), int(screen.height * scale))
    screen = screen.resize(new_size, Image.Resampling.LANCZOS).convert("RGBA")
    x = (WIDTH - screen.width) // 2
    y = top + (available_height - screen.height) // 2

    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)
    shadow_draw.rounded_rectangle(
        (x + 10, y + 16, x + screen.width + 10, y + screen.height + 16),
        radius=radius,
        fill=(0, 0, 0, 52),
    )
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(18)))
    canvas.paste(screen, (x, y), rounded_mask(screen.size, radius))


def card(filename: str, title: str, screen_key: str, *, dark: bool = False, subtitle: str | None = None) -> None:
    background = (18, 20, 25, 255) if dark else (246, 248, 252, 255)
    foreground = (242, 244, 249, 255) if dark else (28, 31, 35, 255)
    accent = (184, 204, 255, 255) if dark else (53, 104, 181, 255)
    canvas = Image.new("RGBA", (WIDTH, HEIGHT), background)
    draw = ImageDraw.Draw(canvas)

    title_font = fitted_font(draw, title, 940, 64, 42, bold=True)
    title_box = draw.textbbox((0, 0), title, font=title_font)
    draw.text(((WIDTH - (title_box[2] - title_box[0])) // 2, 78), title, fill=foreground, font=title_font)

    if subtitle:
        subtitle_font = fitted_font(draw, subtitle, 900, 34, 28, bold=False)
        subtitle_box = draw.textbbox((0, 0), subtitle, font=subtitle_font)
        draw.text(
            ((WIDTH - (subtitle_box[2] - subtitle_box[0])) // 2, 160),
            subtitle,
            fill=accent,
            font=subtitle_font,
        )
        top = 270
    else:
        top = 235

    place_screen(canvas, Image.open(SCREENSHOTS[screen_key]).convert("RGBA"), top=top)
    canvas.convert("RGB").save(OUTPUT / filename, "PNG", optimize=True)


def light_dark_card() -> None:
    canvas = Image.new("RGBA", (WIDTH, HEIGHT), (246, 248, 252, 255))
    draw = ImageDraw.Draw(canvas)
    title = "Светлая и тёмная темы"
    title_font = fitted_font(draw, title, 940, 64, 42, bold=True)
    title_box = draw.textbbox((0, 0), title, font=title_font)
    draw.text(
        ((WIDTH - (title_box[2] - title_box[0])) // 2, 78),
        title,
        fill=(28, 31, 35, 255),
        font=title_font,
    )

    images = [
        Image.open(SCREENSHOTS["calendar_light"]).convert("RGBA"),
        Image.open(SCREENSHOTS["calendar_dark"]).convert("RGBA"),
    ]
    for index, image in enumerate(images):
        crop_height = int(image.height * 0.72)
        image = image.crop((0, 0, image.width, crop_height))
        target_width = 470
        scale = target_width / image.width
        image = image.resize((target_width, int(image.height * scale)), Image.Resampling.LANCZOS)
        x = 55 if index == 0 else 555
        y = 285

        shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
        shadow_draw = ImageDraw.Draw(shadow)
        shadow_draw.rounded_rectangle(
            (x + 8, y + 12, x + image.width + 8, y + image.height + 12),
            radius=30,
            fill=(0, 0, 0, 45),
        )
        canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(16)))
        canvas.paste(image, (x, y), rounded_mask(image.size, 30))

    canvas.convert("RGB").save(OUTPUT / "06-light-dark.png", "PNG", optimize=True)


def main() -> None:
    icon = Image.open(ROOT / "app/src/main/res/drawable-nodpi/ic_launcher_exact.webp").convert("RGB")
    icon.resize((512, 512), Image.Resampling.LANCZOS).save(
        OUTPUT / "rustore-icon-512.png",
        "PNG",
        optimize=True,
    )

    card("01-calendar.png", "Смены, часы и доход — в календаре", "calendar_light")
    card("02-dark-theme.png", "Спокойная тёмная тема", "calendar_dark", dark=True)
    card("03-year-summary.png", "Итоги за месяц и год", "year_light")
    card("04-large-font.png", "Крупный шрифт без потери данных", "year_large")
    card(
        "05-local-first.png",
        "Без аккаунта, рекламы и интернета",
        "year_dark",
        dark=True,
        subtitle="Данные остаются на устройстве",
    )
    light_dark_card()

    for path in sorted(OUTPUT.glob("*.png")):
        image = Image.open(path)
        if path.name == "rustore-icon-512.png":
            assert image.size == (512, 512)
            assert path.stat().st_size <= 1 * 1024 * 1024
        else:
            assert image.size == (1080, 1920)
            assert path.stat().st_size <= 3 * 1024 * 1024


if __name__ == "__main__":
    main()
