{ pkgs ? import (builtins.getFlake "nixpkgs") {
    config = {
      allowUnfree = true;
      android_sdk.accept_license = true;
    };
  }
}:

let
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
pkgs.mkShell {
  packages = with pkgs; [
    jdk
    gradle_9
    kotlin
    androidSdk
    git
    zip
    unzip
    steam-run
    aapt2Fhs
  ];

  JAVA_HOME = "${jdk}";
  # androidSdk is a Nix wrapper; AGP needs the actual SDK root with the
  # standard platforms/, build-tools/ and licenses/ directories.
  ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
  ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";

  shellHook = ''
    export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/22.0/bin:$ANDROID_HOME/build-tools/37.0.0:$PATH"
    export GRADLE_OPTS="''${GRADLE_OPTS:-} -Dorg.gradle.project.android.aapt2FromMavenOverride=${aapt2Fhs}/bin/aapt2"
  '';
}
