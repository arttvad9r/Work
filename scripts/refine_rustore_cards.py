#!/usr/bin/env python3
"""Final composition refinements for the premium RuStore gallery."""

from pathlib import Path
import sys

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import generate_rustore_assets as g  # noqa: E402


def hero_card() -> None:
    canvas = g.background(0)
    g.draw_heading(canvas, "Рабочий месяц — одним взглядом", "Смены · часы · доход")
    screen = Image.open(g.SCREENSHOTS["calendar_light"]()).convert("RGBA")

    g.shadowed_panel(
        canvas,
        screen,
        center=(555, 1300),
        width=780,
        angle=-2.0,
        shadow_alpha=58,
    )
    g.pill(
        canvas,
        "6 смен · 47:30 · 23 725 ₽",
        (430, 700),
        accent=True,
    )
    g.save(canvas, "01-hero-calendar.png")


def privacy_card() -> None:
    canvas = g.background(4, dark=True)
    g.draw_heading(
        canvas,
        "Рабочие данные остаются на устройстве",
        "Без аккаунта, рекламы и обязательного интернета",
        dark=True,
        title_size=64,
    )
    privacy = Image.open(g.SCREENSHOTS["privacy_dark"]()).convert("RGBA")
    privacy = g.crop_norm(privacy, 0.0, 0.0, 1.0, 0.76)
    calendar = Image.open(g.SCREENSHOTS["calendar_dark"]()).convert("RGBA")
    calendar_crop = g.crop_norm(calendar, 0.04, 0.12, 0.96, 0.78)

    g.shadowed_panel(
        canvas,
        calendar_crop,
        center=(310, 1320),
        width=560,
        radius=44,
        angle=-4.5,
        shadow_alpha=50,
        border=(42, 48, 61, 255),
    )
    g.shadowed_panel(
        canvas,
        privacy,
        center=(730, 1250),
        width=680,
        radius=44,
        angle=2.4,
        shadow_alpha=64,
        border=(48, 54, 68, 255),
    )
    g.pill(canvas, "локально на устройстве", (86, 700), dark=True, accent=True)
    g.save(canvas, "05-privacy.png")


def export_card() -> None:
    canvas = g.background(5)
    g.draw_heading(canvas, "Экспортируй, когда нужно", "JSON для восстановления · CSV для таблиц")
    export = Image.open(g.SCREENSHOTS["export"]()).convert("RGBA")
    g.shadowed_panel(
        canvas,
        export,
        center=(620, 1220),
        width=800,
        radius=44,
        angle=1.2,
        shadow_alpha=60,
    )
    g.pill(canvas, "JSON", (100, 735), accent=True)
    g.pill(canvas, "CSV", (255, 735))
    g.save(canvas, "06-export.png")


def main() -> None:
    hero_card()
    privacy_card()
    export_card()


if __name__ == "__main__":
    main()
