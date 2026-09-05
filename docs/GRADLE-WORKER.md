# Gradle test worker diagnostics (Windows / JDK 21)

`ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain` happens before test assertions. It is not evidence of a failing NBT roundtrip, nor evidence that JUnit passed. Non-ASCII paths, a corrupt worker cache, Java launch settings, or antivirus interference are possible causes; the reported log alone does not establish the root cause.

## Isolated local reproduction

From the repository root in PowerShell, with JDK 21 available:

```powershell
.\tests\run-windows.ps1 -GradleHome D:\GradleHome -TempHome D:\GradleTmp
```

Choose existing drives and directories you can write to. The script creates these directories, temporarily sets `GRADLE_USER_HOME`, `TEMP`, `TMP` and `java.io.tmpdir`, runs a clean build with tests (no test exclusions), checks the JUnit XML, and restores the original environment variables. It does not delete the existing user cache or change machine-wide settings. It requires PowerShell script execution to be allowed by your normal local policy; do not disable organization security policies to run it.

For manual reproduction:

```powershell
$env:GRADLE_USER_HOME = 'D:\GradleHome'
$env:TEMP = 'D:\GradleTmp'
$env:TMP = 'D:\GradleTmp'
New-Item -ItemType Directory -Force D:\GradleHome,D:\GradleTmp
.\gradlew.bat --no-daemon --no-build-cache --rerun-tasks --stacktrace clean build
```

If it still fails, retain the full stacktrace, `java -version`, the selected JDK path, and any `worker-error-*.txt` under the isolated Gradle home. Check that the repository path and JDK path are ASCII as well. Do not use `-x test` as a release workaround.

## CI evidence

The build workflow uses a fresh hosted Ubuntu runner, JDK 21, an isolated ASCII Gradle home and Java temporary directory. It runs:

```bash
./gradlew --no-daemon --no-build-cache --rerun-tasks --stacktrace clean build
python3 tests/verify_results.py
```

The second command fails unless all four expected `VehicleStateTest` methods actually appear in the JUnit report without failure, error or skip. Reports, worker diagnostics and a commit-stamped verification JSON are uploaded; the jar is only uploaded after all preceding steps succeed.

A committed workflow is not a successful run. Verify the Actions run for the exact final commit before calling build/tests green. The in-game client QA remains separate.
