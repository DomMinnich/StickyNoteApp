# This is what I used to compile the Java code into a runnable JAR file and then into an EXE file using Launch4j.
# It assumes you have Java and Launch4j installed on your system.

# run it by running the command `python compile.py` in the terminal
# then run the command `java -jar dist/StickyNoteApp.jar` to run the JAR file

# compile.py

import os
import subprocess
import shutil

# Paths
JAVA_SRC_DIR = "src"
OUTPUT_DIR = "dist"
BIN_DIR = "bin"
JAR_FILE = "StickyNoteApp.jar"
EXE_FILE = "StickyNoteApp.exe"
LAUNCH4J_CONFIG = "launch4j_config.xml"
LAUNCH4J_PATH = "C:\\Program Files (x86)\\Launch4j"
MANIFEST_FILE = "manifest.txt"

def create_manifest():
    """Creates a manifest file with the Main-Class attribute."""
    with open(MANIFEST_FILE, "w") as f:
        f.write("Main-Class: Main\n")

def compile_java():
    """Compiles Java source files into class files."""
    print("Compiling Java source files...")
    if not os.path.exists(BIN_DIR):
        os.makedirs(BIN_DIR)
    subprocess.run(["javac", "-d", BIN_DIR, "-sourcepath", JAVA_SRC_DIR, f"{JAVA_SRC_DIR}/Main.java"], check=True)
    print("Compilation complete.")

def create_jar():
    """Packages compiled class files and resources into a JAR file."""
    print("Creating JAR file...")
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)
    create_manifest()

    # Ensure the notes_rtf folder exists
    rtf_folder = os.path.join(JAVA_SRC_DIR, "notes_rtf")
    if not os.path.exists(rtf_folder):
        os.makedirs(rtf_folder)

    # Package the compiled class files and the notes_rtf folder into the JAR
    subprocess.run([
        "jar", "cmf", MANIFEST_FILE, os.path.join(OUTPUT_DIR, JAR_FILE),
        "-C", BIN_DIR, ".", "-C", JAVA_SRC_DIR, "notes_rtf"
    ], check=True)
    print(f"JAR file created at {os.path.join(OUTPUT_DIR, JAR_FILE)}.")

def create_exe():
    """Wraps the JAR file into a Windows executable using Launch4j."""
    print("Creating EXE file...")
    if shutil.which("launch4j") is None:
        os.environ["PATH"] += os.pathsep + LAUNCH4J_PATH
        if shutil.which("launch4j") is None:
            raise RuntimeError("Launch4j is not installed or not in PATH. Please install it to proceed.")
    
    # Generate Launch4j configuration file
    with open(LAUNCH4J_CONFIG, "w") as config:
        config.write(f"""
<launch4jConfig>
    <dontWrapJar>false</dontWrapJar>
    <headerType>gui</headerType>
    <jar>{os.path.abspath(os.path.join(OUTPUT_DIR, JAR_FILE))}</jar>
    <outfile>{os.path.abspath(os.path.join(OUTPUT_DIR, EXE_FILE))}</outfile>
    <errTitle>StickyNoteApp Error</errTitle>
    <cmdLine></cmdLine>
    <chdir>.</chdir>
    <priority>normal</priority>
    <downloadUrl>https://adoptopenjdk.net/</downloadUrl>
    <supportUrl></supportUrl>
    <stayAlive>false</stayAlive>
    <manifest></manifest>
    <icon></icon>
    <log>{os.path.abspath(os.path.join(OUTPUT_DIR, "launch4j.log"))}</log>
    <jre>
        <path></path>
        <bundledJre64Bit>false</bundledJre64Bit>
        <minVersion>1.8.0</minVersion>
        <maxVersion></maxVersion>
        <jdkPreference>preferJre</jdkPreference>
        <runtimeBits>64/32</runtimeBits>
    </jre>
</launch4jConfig>
        """)
    
    # Run Launch4j
    subprocess.run(["launch4j", LAUNCH4J_CONFIG], check=True)
    print(f"EXE file created at {os.path.join(OUTPUT_DIR, EXE_FILE)}.")

def main():
    try:
        compile_java()
        create_jar()
        create_exe()
        print("Build process completed successfully.")
    except subprocess.CalledProcessError as e:
        print(f"Error during build process: {e}")
    finally:
        # Clean up temporary files
        if os.path.exists(LAUNCH4J_CONFIG):
            os.remove(LAUNCH4J_CONFIG)
        if os.path.exists(MANIFEST_FILE):
            os.remove(MANIFEST_FILE)

if __name__ == "__main__":
    main()