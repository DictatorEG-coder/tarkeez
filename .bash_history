termux-setup-storage
y
ls
ls ~/storage/shared
echo TEST
pkg update && pkg upgrade -y
echo N
N
termux-setup-storage
y
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~git --version && unzip -v | head -1
pkg install git unzip
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
git --version && unzip -v | head -1
ls ~/storage/downloads | grep tahmeel~
ls ~/storage/downloads | grep tahmeel
cd ~
unzip -q ~/storage/downloads/tahmeel.zip
cd tahmeel
ls
pwd
git config --global user.name "DictatorEG-coder"
git config --global user.email "any@example.com"
git init -b main
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/DictatorEG-coder/tahmeel.git
git push -u origin main
cd ~/tahmeel
sed -i 's/^  android:$/  android:\n    permissions:\n      contents: write/' .github/workflows/ci.yml
cat >> .github/workflows/ci.yml <<'EOF'
      - name: Publish APK to Releases
        if: github.ref == 'refs/heads/main'
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          cp build/app/outputs/flutter-apk/app-debug.apk tahmeel.apk
          gh release delete latest-debug --cleanup-tag --yes || true
          gh release create latest-debug tahmeel.apk --title "Tahmeel debug build" --notes "Automatic debug build" --prerelease --target "$GITHUB_SHA"
EOF

git add .
git commit -m "Publish APK to Releases"
git push
cd ~
unzip -q ~/storage/downloads/tarkeez.zip
cd tarkeez
git init -b main
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/DictatorEG-coder/tarkeez.git
git push -u origin main
