#!/usr/bin/env python3
from pathlib import Path
import os, subprocess, tempfile
root = Path(__file__).resolve().parents[1]
java_home = os.environ.get("JAVA_HOME")
def tool(name): return str(Path(java_home)/"bin"/name) if java_home else name
with tempfile.TemporaryDirectory() as target:
    subprocess.run([tool("javac"), "-d", target, str(root/"app/src/main/java/org/telesin/buttonlab/WakePolicy.java"), str(root/"tests/WakePolicyTest.java")], check=True)
    subprocess.run([tool("java"), "-cp", target, "org.telesin.buttonlab.WakePolicyTest"], check=True)
