{
  description = "WorkTime Android development environment";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ];
      forAllSystems = f: nixpkgs.lib.genAttrs systems (system: f system);
    in
    {
      devShells = forAllSystems (system:
        let
          pkgs = import nixpkgs {
            inherit system;
            config = {
              allowUnfree = true;
              android_sdk.accept_license = true;
            };
          };
          androidPackages = pkgs.androidenv.composeAndroidPackages {
            platformVersions = [ "35" "37" ];
            buildToolsVersions = [ "37.0.0" ];
            includeNDK = false;
            includeEmulator = true;
            includeSystemImages = false;
          };
          androidSdk = androidPackages.androidsdk;
          jdk = pkgs.jdk17;
          aapt2Fhs = pkgs.writeShellScriptBin "aapt2" ''
            exec ${pkgs.steam-run}/bin/steam-run \
              ${androidSdk}/libexec/android-sdk/build-tools/37.0.0/aapt2 "$@"
          '';
        in
        {
          default = pkgs.mkShell {
            packages = with pkgs; [
              jdk
              gradle_9
              kotlin
              androidSdk
              git
              python3
              zip
              unzip
              steam-run
              aapt2Fhs
            ];

            JAVA_HOME = "${jdk}";
            ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
            ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";

            shellHook = ''
              export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/22.0/bin:$ANDROID_HOME/build-tools/37.0.0:$PATH"
              export GRADLE_OPTS="''${GRADLE_OPTS:-} -Dorg.gradle.project.android.aapt2FromMavenOverride=${aapt2Fhs}/bin/aapt2"
            '';
          };
        });
    };
}
