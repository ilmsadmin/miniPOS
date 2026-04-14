/**
 * ViPOS — Google Play Screenshot Capture Script
 * Captures all 8 screenshot HTML files as 1080×1920 PNG images
 * Also captures the feature-graphic.html as 1024×500
 * 
 * Usage: node capture.js
 */

const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

const SCREENSHOT_FILES = [
  { html: 'ss-01-pos.html',       output: 'ss-01-pos.png',       w: 1080, h: 1920 },
  { html: 'ss-02-payment.html',   output: 'ss-02-payment.png',   w: 1080, h: 1920 },
  { html: 'ss-03-reports.html',   output: 'ss-03-reports.png',   w: 1080, h: 1920 },
  { html: 'ss-04-inventory.html', output: 'ss-04-inventory.png', w: 1080, h: 1920 },
  { html: 'ss-05-products.html',  output: 'ss-05-products.png',  w: 1080, h: 1920 },
  { html: 'ss-06-orders.html',    output: 'ss-06-orders.png',    w: 1080, h: 1920 },
  { html: 'ss-07-multiuser.html', output: 'ss-07-multiuser.png', w: 1080, h: 1920 },
  { html: 'ss-08-settings.html',  output: 'ss-08-settings.png',  w: 1080, h: 1920 },
];

// Also capture feature graphic
const FEATURE_GRAPHIC = {
  html: '../feature-graphic.html',
  output: 'feature-graphic.png',
  w: 1024,
  h: 500,
};

const OUTPUT_DIR = path.join(__dirname, 'output');

async function captureScreenshot(browser, file) {
  const page = await browser.newPage();
  
  // Set viewport to exact screenshot dimensions
  await page.setViewport({
    width: file.w,
    height: file.h,
    deviceScaleFactor: 1,
  });

  const htmlPath = path.resolve(__dirname, file.html);
  
  if (!fs.existsSync(htmlPath)) {
    console.log(`  ⚠️  Skipped: ${file.html} (file not found)`);
    await page.close();
    return false;
  }

  const fileUrl = `file://${htmlPath}`;
  
  // Navigate and wait for fonts + images to load
  await page.goto(fileUrl, { 
    waitUntil: 'networkidle0',
    timeout: 30000,
  });

  // Extra wait for web fonts to render
  await page.waitForFunction(() => document.fonts.ready);
  await new Promise(r => setTimeout(r, 1500));

  // Find the .screenshot or .feature-graphic element
  const selector = file.w === 1024 ? '.feature-graphic' : '.screenshot';
  const element = await page.$(selector);
  
  if (element) {
    // Capture just the element
    const outputPath = path.join(OUTPUT_DIR, file.output);
    await element.screenshot({
      path: outputPath,
      type: 'png',
    });
    console.log(`  ✅ ${file.output} (${file.w}×${file.h})`);
  } else {
    // Fallback: capture full viewport
    const outputPath = path.join(OUTPUT_DIR, file.output);
    await page.screenshot({
      path: outputPath,
      type: 'png',
      clip: { x: 0, y: 0, width: file.w, height: file.h },
    });
    console.log(`  ✅ ${file.output} (viewport clip ${file.w}×${file.h})`);
  }

  await page.close();
  return true;
}

async function main() {
  console.log('');
  console.log('🎨 ViPOS — Google Play Screenshot Capture');
  console.log('=========================================');
  console.log('');

  // Create output directory
  if (!fs.existsSync(OUTPUT_DIR)) {
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  }

  console.log('🚀 Launching browser...');
  const browser = await puppeteer.launch({
    headless: 'new',
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-web-security',
      '--allow-file-access-from-files',
      '--font-render-hinting=none',
    ],
  });

  // Capture phone screenshots
  console.log('');
  console.log('📱 Capturing phone screenshots (1080×1920)...');
  let count = 0;
  for (const file of SCREENSHOT_FILES) {
    const ok = await captureScreenshot(browser, file);
    if (ok) count++;
  }

  // Capture feature graphic
  console.log('');
  console.log('🖼️  Capturing feature graphic (1024×500)...');
  const fgOk = await captureScreenshot(browser, FEATURE_GRAPHIC);
  if (fgOk) count++;

  await browser.close();

  console.log('');
  console.log(`✨ Done! ${count} images saved to: ${OUTPUT_DIR}`);
  console.log('');
  
  // List output files with sizes
  const files = fs.readdirSync(OUTPUT_DIR).filter(f => f.endsWith('.png'));
  files.forEach(f => {
    const stats = fs.statSync(path.join(OUTPUT_DIR, f));
    const sizeKB = (stats.size / 1024).toFixed(0);
    console.log(`   📄 ${f} — ${sizeKB} KB`);
  });
  console.log('');
}

main().catch(err => {
  console.error('❌ Error:', err.message);
  process.exit(1);
});
