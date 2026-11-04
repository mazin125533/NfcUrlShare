# NFC URL共有

Chrome の「共有」で受け取った URL を、NFC で別のスマホに渡す Android アプリです。
HCE（Host Card Emulation）で **NFC Forum Type 4 タグ**をエミュレートし、URI レコードの NDEF を返します。
受け取る側はアプリ不要で、ふつうの NFC タグを読んだときと同じようにブラウザで URL が開きます（Android / iPhone XS 以降）。

## 使い方
1. Chrome で共有したいページを開き、「共有」→「NFC URL共有」を選ぶ
2. 画面に「相手のスマホの背面にかざしてください」と出たら、相手のスマホと背面を合わせる
3. 相手のスマホで URL が開く

URL を決定すると（共有から受け取ったとき・「この URL を使う」を押したとき）QR コードも表示されます。NFC が使えない相手はカメラで読み取ってください。

使った URL は「履歴」に新しい順で最大20件残ります（端末内にのみ保存）。タップでもう一度共有、長押しで削除できます。

- 相手が読み取ると「ブブッ」と短く振動して知らせます
- この画面を開いている間だけタグとして応答します
- 相手の端末は画面ロックを解除し、NFC をオンにしてください

## 必要なもの
- Android 8.0 以上、NFC / HCE 対応端末

## ビルド
JDK 17 と Android SDK（API 35）が必要です。
```
./gradlew assembleDebug
```
APK は `app/build/outputs/apk/debug/` に出力されます。

## ライブラリ
- [ZXing core](https://github.com/zxing/zxing)（Apache License 2.0）: QR コード生成

## リリース（GitHub Actions）
`v*` タグを push すると、署名付きの APK がビルドされて GitHub Release に添付されます。
```
git tag v1.0.0
git push origin v1.0.0
```
main への push と PR でもリリース APK をビルドし、Actions の Artifacts に保存します。

### 署名鍵の準備（最初に1回だけ）
```
powershell -ExecutionPolicy Bypass -File tools\setup-release-signing.ps1
```
鍵を `%USERPROFILE%\.keystores` に作成し、GitHub Secrets（`ANDROID_KEYSTORE_BASE64` / `ANDROID_KEYSTORE_PASSWORD` / `ANDROID_KEY_ALIAS` / `ANDROID_KEY_PASSWORD`）に登録します。
**鍵とパスワードのファイルは必ずバックアップしてください。** 失うと同じアプリとして更新できなくなります。

## ライセンス

[MIT License](LICENSE)。自由に使用・改変・再配布できます。
