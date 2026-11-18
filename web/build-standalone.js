const fs = require('fs');
const path = require('path');
const QRCode = require('./node_modules/qrcode');

(async () => {
  const html = fs.readFileSync(path.join(__dirname, 'public', 'index.html'), 'utf8');
  const css = fs.readFileSync(path.join(__dirname, 'public', 'styles.css'), 'utf8');
  const jsqr = fs.readFileSync(path.join(__dirname, 'node_modules', 'jsqr', 'dist', 'jsQR.js'), 'utf8');
  const analyzer = fs.readFileSync(path.join(__dirname, 'public', 'analyzer.js'), 'utf8');
  let app = fs.readFileSync(path.join(__dirname, 'public', 'app.js'), 'utf8');

  const urls = [
    'http://insecure-example.com/portal',
    'http://192.168.1.1/login-verify',
    'https://bit.ly/3xSampleRedirect',
    'https://secure-login-verify-account-confirm.com/banking',
    'http://google.com@free-gift-claim.xyz/secure-update',
    'https://www.google.com'
  ];

  const qrMap = {};
  for (const u of urls) {
    qrMap[u] = await QRCode.toDataURL(u, {
      width: 280,
      margin: 2,
      color: { dark: '#0f172a', light: '#ffffff' }
    });
  }

  // Replace server QR fetch with embedded offline QR map
  app = app.replace(
    /try \{\s*const resp = await fetch\(`\/api\/qrcode\?text=\$\{encodeURIComponent\(scenario\.url\)\}`\);[\s\S]*?\} catch \(_\) \{\s*showToast\('Failed to generate test QR image'\);\s*\}/,
    `const embeddedDataUrl = window.__EMBEDDED_QR_CODES__ && window.__EMBEDDED_QR_CODES__[scenario.url];
    if (embeddedDataUrl) {
      qrPreviewImg.src = embeddedDataUrl;
      btnQrDownload.href = embeddedDataUrl;
    } else {
      showToast('QR preview unavailable');
    }`
  );

  // Escape any closing script tags inside bundled libraries
  const safeJsqr = jsqr.replace(/<\/script>/gi, '<\\/script>');
  const safeAnalyzer = analyzer.replace(/<\/script>/gi, '<\\/script>');
  const safeApp = app.replace(/<\/script>/gi, '<\\/script>');

  const standalone = html
    .replace('<link rel="stylesheet" href="/styles.css" />', () => `<style>\n${css}\n</style>`)
    .replace(
      '<script src="/vendor/jsQR.js"></script>',
      () => `<script>\nwindow.__EMBEDDED_QR_CODES__ = ${JSON.stringify(qrMap)};\n${safeJsqr}\n</script>`
    )
    .replace('<script src="/analyzer.js"></script>', () => `<script>\n${safeAnalyzer}\n</script>`)
    .replace('<script src="/app.js"></script>', () => `<script>\n${safeApp}\n</script>`);

  const outPath = path.resolve(__dirname, '..', 'QR_Safety_Scanner.html');
  fs.writeFileSync(outPath, standalone, 'utf8');
  console.log('Successfully created standalone file:', outPath, `(${Math.round(standalone.length / 1024)} KB)`);
})();
