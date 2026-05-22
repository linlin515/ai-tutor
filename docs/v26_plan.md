## PM Planning Output for v2.6
**Current signing config:** debug.keystore auto-generated
**Required changes:**
1. Generate formal release keystore
2. Update build.gradle.kts to support release signing with env vars
3. Update CI workflow to use secrets (already referencing them)
4. Ensure keystore file is not committed (gitignore)

**Files to modify:**
- android/app/build.gradle.kts
- .github/workflows/android-ci.yml (already correct, just need secrets set)
- Ensure debug.keystore is in .gitignore (it is?)

**Steps for Coder:**
1. Create release keystore (or instruct to generate via keytool)
2. Update build.gradle.kts signingConfigs to use env vars for release
3. Verify .gitignore excludes *.keystore, *.jks
4. No changes needed to CI workflow (already references secrets)
5. Test assembleRelease with env vars set

**Estimated effort:** 0.5 day
