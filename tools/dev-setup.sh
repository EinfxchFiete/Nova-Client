# Sets up a cloud/Linux dev session: Java 25 (Temurin) + XDG_RUNTIME_DIR for Xvfb tests.
# Usage:  source tools/dev-setup.sh [tool-dir]      (default: ~/.nova-dev)
NOVA_DEV_DIR="${1:-$HOME/.nova-dev}"
mkdir -p "$NOVA_DEV_DIR"
if [ ! -x "$NOVA_DEV_DIR/jdk25/bin/java" ]; then
  curl -sSL -o "$NOVA_DEV_DIR/jdk25.tar.gz" "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse" \
    && mkdir -p "$NOVA_DEV_DIR/jdk25" && tar xzf "$NOVA_DEV_DIR/jdk25.tar.gz" -C "$NOVA_DEV_DIR/jdk25" --strip-components=1
fi
export JAVA_HOME="$NOVA_DEV_DIR/jdk25"
export PATH="$JAVA_HOME/bin:$PATH"
export XDG_RUNTIME_DIR="$NOVA_DEV_DIR/xdg"
mkdir -p "$XDG_RUNTIME_DIR" && chmod 700 "$XDG_RUNTIME_DIR"
java -version 2>&1 | tail -1
