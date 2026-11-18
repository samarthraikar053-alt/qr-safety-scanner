(() => {
  const HISTORY_KEY = 'qr_safety_scanner_history_v1';

  // State
  let stream = null;
  let scanningActive = false;
  let scanningPaused = false;
  let rafId = null;
  let facingMode = 'environment';
  let torchEnabled = false;
  let currentAnalysis = null;
  let historyItems = loadHistory();

  // DOM Elements
  const tabs = document.querySelectorAll('.nav-tab');
  const views = {
    scanner: document.getElementById('tab-scanner'),
    sandbox: document.getElementById('tab-sandbox'),
    history: document.getElementById('tab-history')
  };

  const video = document.getElementById('camera-video');
  const canvas = document.getElementById('scan-canvas');
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  const placeholder = document.getElementById('camera-placeholder');
  const placeholderTitle = document.getElementById('placeholder-title');
  const placeholderDesc = document.getElementById('placeholder-desc');
  const scannerHint = document.getElementById('scanner-hint');

  const btnStartCamera = document.getElementById('btn-start-camera');
  const btnToggleCamera = document.getElementById('btn-toggle-camera');
  const labelCameraToggle = document.getElementById('label-camera-toggle');
  const btnTorch = document.getElementById('btn-torch');
  const btnSwitchCamera = document.getElementById('btn-switch-camera');
  const btnUploadImage = document.getElementById('btn-upload-image');
  const fileInput = document.getElementById('qr-file-input');

  const quickUrlInput = document.getElementById('quick-url-input');
  const btnQuickCheck = document.getElementById('btn-quick-check');

  const historyListEl = document.getElementById('history-list');
  const historyCountEl = document.getElementById('history-count');
  const btnClearHistory = document.getElementById('btn-clear-history');
  const sandboxListEl = document.getElementById('sandbox-list');

  // Verdict Modal
  const verdictModal = document.getElementById('verdict-modal');
  const modalIcon = document.getElementById('modal-icon');
  const modalTitle = document.getElementById('modal-title');
  const modalVerdictPill = document.getElementById('modal-verdict-pill');
  const modalUrlText = document.getElementById('modal-url-text');
  const modalHostText = document.getElementById('modal-host-text');
  const modalWarningsSection = document.getElementById('modal-warnings-section');
  const modalWarningsHeading = document.getElementById('modal-warnings-heading');
  const modalWarningsList = document.getElementById('modal-warnings-list');
  const btnCopyUrl = document.getElementById('btn-copy-url');
  const btnDontOpen = document.getElementById('btn-dont-open');
  const btnOpenAnyway = document.getElementById('btn-open-anyway');

  // QR Preview Modal
  const qrPreviewModal = document.getElementById('qr-preview-modal');
  const qrPreviewTitle = document.getElementById('qr-preview-title');
  const qrPreviewImg = document.getElementById('qr-preview-img');
  const qrPreviewUrl = document.getElementById('qr-preview-url');
  const btnQrTestNow = document.getElementById('btn-qr-test-now');
  const btnQrDownload = document.getElementById('btn-qr-download');
  const btnQrClose = document.getElementById('btn-qr-close');
  let activePreviewUrl = '';

  const toastEl = document.getElementById('toast');
  let toastTimer = null;

  const TEST_SCENARIOS = [
    {
      title: '1. Missing HTTPS (Plain HTTP)',
      desc: 'Triggers the unencrypted HTTP warning flag.',
      url: 'http://insecure-example.com/portal',
      expected: 'HIGH_RISK',
      expectedLabel: 'High Risk'
    },
    {
      title: '2. IP Address Host + Phishing Keyword',
      desc: 'Triggers numeric IP host, plain HTTP, and "login-verify".',
      url: 'http://192.168.1.1/login-verify',
      expected: 'HIGH_RISK',
      expectedLabel: 'High Risk'
    },
    {
      title: '3. Link Shortener (Obfuscation)',
      desc: 'Triggers known URL shortener obfuscation warning.',
      url: 'https://bit.ly/3xSampleRedirect',
      expected: 'SUSPICIOUS',
      expectedLabel: 'Suspicious'
    },
    {
      title: '4. Suspicious Phishing Keywords',
      desc: 'Triggers odd keywords like "login-verify" & "account-confirm".',
      url: 'https://secure-login-verify-account-confirm.com/banking',
      expected: 'HIGH_RISK',
      expectedLabel: 'High Risk'
    },
    {
      title: "5. Deceptive '@' Host Spoof",
      desc: 'Triggers userinfo host spoofing and risky .xyz TLD.',
      url: 'http://google.com@free-gift-claim.xyz/secure-update',
      expected: 'HIGH_RISK',
      expectedLabel: 'High Risk'
    },
    {
      title: '6. Legitimate Verified HTTPS Link',
      desc: 'Clean HTTPS domain with zero warning signs.',
      url: 'https://www.google.com',
      expected: 'SAFE',
      expectedLabel: 'Safe'
    }
  ];

  // Utility: Toast
  function showToast(msg) {
    toastEl.textContent = msg;
    toastEl.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toastEl.classList.remove('show'), 2600);
  }

  // Utility: Escape HTML
  function escapeHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // Tab Navigation
  tabs.forEach((btn) => {
    btn.addEventListener('click', () => {
      const target = btn.dataset.tab;
      tabs.forEach((t) => t.classList.toggle('active', t === btn));
      Object.keys(views).forEach((k) => views[k].classList.toggle('active', k === target));
    });
  });

  // Native BarcodeDetector (hardware ML Kit on Android Chrome)
  let nativeDetector = null;
  if ('BarcodeDetector' in window) {
    try {
      nativeDetector = new window.BarcodeDetector({ formats: ['qr_code'] });
    } catch (_) {
      nativeDetector = null;
    }
  }
  let detectingFrame = false;

  // Camera Management
  async function startCamera(silent = false) {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      placeholderTitle.textContent = 'HTTPS Required for Live Camera';
      placeholderDesc.textContent = 'Mobile browsers require an HTTPS link (not a local file:// or WhatsApp content:// file) to open the live camera.';
      if (!silent) showToast('Open via the HTTPS link to use live camera');
      return;
    }

    stopCamera(false);
    const constraintCandidates = [
      {
        video: {
          facingMode: { ideal: facingMode },
          width: { ideal: 1280 },
          height: { ideal: 720 }
        },
        audio: false
      },
      { video: { facingMode }, audio: false },
      { video: true, audio: false }
    ];

    let acquiredStream = null;
    let lastErr = null;
    for (const constraints of constraintCandidates) {
      try {
        acquiredStream = await navigator.mediaDevices.getUserMedia(constraints);
        break;
      } catch (e) {
        lastErr = e;
      }
    }

    if (!acquiredStream) {
      console.error(lastErr);
      placeholder.classList.remove('hidden');
      placeholderTitle.textContent = 'Camera Permission Required';
      placeholderDesc.textContent = 'Tap "Start Camera Scanner" and allow camera permission in your browser.';
      if (!silent) showToast('Could not access camera — check browser permissions');
      return;
    }

    try {
      stream = acquiredStream;
      video.setAttribute('playsinline', 'true');
      video.setAttribute('autoplay', 'true');
      video.muted = true;
      video.srcObject = stream;
      await video.play();

      scanningActive = true;
      scanningPaused = false;
      placeholder.classList.add('hidden');
      labelCameraToggle.textContent = 'Stop Cam';
      btnToggleCamera.classList.add('active');
      btnTorch.disabled = false;
      btnSwitchCamera.disabled = false;
      scannerHint.textContent = 'Scanning... Align QR code within frame';

      tickScan();
    } catch (err) {
      console.error(err);
      stopCamera(true);
      if (!silent) showToast('Tap Start Camera Scanner to begin');
    }
  }

  function stopCamera(resetPlaceholder = true) {
    scanningActive = false;
    torchEnabled = false;
    btnTorch.classList.remove('active');
    btnTorch.disabled = true;
    btnSwitchCamera.disabled = true;
    btnToggleCamera.classList.remove('active');
    labelCameraToggle.textContent = 'Start Cam';

    if (rafId) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
    if (stream) {
      stream.getTracks().forEach((t) => t.stop());
      stream = null;
    }
    video.srcObject = null;

    if (resetPlaceholder) {
      placeholder.classList.remove('hidden');
      placeholderTitle.textContent = 'Camera Paused';
      placeholderDesc.textContent = 'Tap below to resume live QR camera scanning, or upload a QR image.';
    }
  }

  function tickScan() {
    if (!scanningActive) return;

    if (!scanningPaused && !detectingFrame && video.readyState >= video.HAVE_CURRENT_DATA && video.videoWidth > 0) {
      if (nativeDetector) {
        detectingFrame = true;
        nativeDetector
          .detect(video)
          .then((codes) => {
            if (!scanningPaused && codes && codes.length > 0 && codes[0].rawValue) {
              scanningPaused = true;
              handleDetectedContent(codes[0].rawValue.trim(), true);
            }
          })
          .catch(() => {
            nativeDetector = null;
          })
          .finally(() => {
            detectingFrame = false;
          });
      } else {
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        if (typeof jsQR === 'function') {
          const code = jsQR(imageData.data, imageData.width, imageData.height, {
            inversionAttempts: 'dontInvert'
          });
          if (code && code.data && code.data.trim()) {
            scanningPaused = true;
            handleDetectedContent(code.data.trim(), true);
          }
        }
      }
    }

    rafId = requestAnimationFrame(tickScan);
  }

  // Flashlight Toggle
  async function toggleTorch() {
    if (!stream) return;
    const track = stream.getVideoTracks()[0];
    if (!track) return;

    const capabilities = track.getCapabilities ? track.getCapabilities() : {};
    if (!capabilities.torch) {
      showToast('Flashlight not supported on this camera');
      return;
    }

    try {
      torchEnabled = !torchEnabled;
      await track.applyConstraints({ advanced: [{ torch: torchEnabled }] });
      btnTorch.classList.toggle('active', torchEnabled);
      showToast(torchEnabled ? 'Flashlight ON' : 'Flashlight OFF');
    } catch (_) {
      torchEnabled = false;
      btnTorch.classList.remove('active');
      showToast('Could not toggle flashlight');
    }
  }

  // Switch Camera (Back <-> Front)
  async function switchCamera() {
    facingMode = facingMode === 'environment' ? 'user' : 'environment';
    showToast(facingMode === 'environment' ? 'Switched to Back Camera' : 'Switched to Front Camera');
    await startCamera();
  }

  // Upload QR Image Decoding
  fileInput.addEventListener('change', (e) => {
    const file = e.target.files && e.target.files[0];
    if (!file) return;
    fileInput.value = '';

    const reader = new FileReader();
    reader.onload = () => {
      const img = new Image();
      img.onload = () => {
        canvas.width = img.width;
        canvas.height = img.height;
        ctx.drawImage(img, 0, 0);
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const code = typeof jsQR === 'function'
          ? jsQR(imageData.data, imageData.width, imageData.height)
          : null;
        if (code && code.data) {
          scanningPaused = true;
          handleDetectedContent(code.data.trim(), true);
        } else {
          showToast('No QR code found in selected image');
        }
      };
      img.src = reader.result;
    };
    reader.readAsDataURL(file);
  });

  // Inspect and Present Verdict Modal
  function handleDetectedContent(rawText, saveToHistory = true) {
    const analysis = UrlSafetyAnalyzer.analyze(rawText);
    currentAnalysis = analysis;

    if (saveToHistory) {
      addHistoryRecord(analysis);
    }

    openVerdictModal(analysis);
  }

  function openVerdictModal(analysis) {
    const iconSvgs = {
      SAFE: '<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="M9 12l2 2 4-4"/></svg>',
      SUSPICIOUS: '<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
      HIGH_RISK: '<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>'
    };

    const titles = {
      SAFE: 'Verified Safe Link',
      SUSPICIOUS: 'Suspicious Link Warning',
      HIGH_RISK: 'High Risk Alert!'
    };

    modalIcon.className = `modal-icon-circle ${analysis.threatLevel}`;
    modalIcon.innerHTML = iconSvgs[analysis.threatLevel];
    modalTitle.textContent = titles[analysis.threatLevel];

    modalVerdictPill.className = `verdict-pill ${analysis.threatLevel}`;
    modalVerdictPill.textContent = analysis.threatLabel;

    modalUrlText.textContent = analysis.parsedUrl;
    modalHostText.textContent = `Host: ${analysis.host}`;

    if (analysis.warnings.length > 0) {
      modalWarningsSection.style.display = 'flex';
      modalWarningsHeading.textContent = analysis.threatLevel === 'SAFE'
        ? 'Security Details'
        : `Flagged Warning Signs (${analysis.warnings.length})`;

      modalWarningsList.innerHTML = analysis.warnings
        .map(
          (w) => `
          <div class="warning-card ${w.severity}">
            <div>
              <div class="warning-title">${escapeHtml(w.title)}</div>
              <div class="warning-desc">${escapeHtml(w.description)}</div>
            </div>
          </div>`
        )
        .join('');
    } else {
      modalWarningsSection.style.display = 'flex';
      modalWarningsHeading.textContent = 'Security Assessment';
      modalWarningsList.innerHTML = `
        <div class="warning-card SAFE">
          <div>
            <div class="warning-title">No Suspicious Patterns Found</div>
            <div class="warning-desc">Uses HTTPS encryption with a standard domain name and no known shortener or phishing indicators.</div>
          </div>
        </div>`;
    }

    btnOpenAnyway.style.display = analysis.isWebUrl ? 'inline-flex' : 'none';
    verdictModal.classList.add('open');
  }

  function closeVerdictModal() {
    verdictModal.classList.remove('open');
    currentAnalysis = null;
    setTimeout(() => {
      scanningPaused = false;
    }, 600);
  }

  // History Persistence
  function loadHistory() {
    try {
      const raw = localStorage.getItem(HISTORY_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch (_) {
      return [];
    }
  }

  function saveHistory() {
    try {
      localStorage.setItem(HISTORY_KEY, JSON.stringify(historyItems));
    } catch (_) {}
    renderHistory();
  }

  function addHistoryRecord(analysis) {
    const record = {
      id: Date.now() + Math.random().toString(16).slice(2),
      url: analysis.parsedUrl,
      host: analysis.host,
      threatLevel: analysis.threatLevel,
      threatLabel: analysis.threatLabel,
      summary: analysis.warnings.map((w) => w.title).join(' · ') || 'No warning flags',
      timestamp: Date.now()
    };
    historyItems.unshift(record);
    if (historyItems.length > 100) historyItems.pop();
    saveHistory();
  }

  function renderHistory() {
    historyCountEl.textContent = historyItems.length;
    btnClearHistory.style.display = historyItems.length ? 'inline-block' : 'none';

    if (historyItems.length === 0) {
      historyListEl.innerHTML = `
        <div class="empty-state">
          <svg width="42" height="42" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
            <circle cx="12" cy="12" r="9"/><polyline points="12 7 12 12 15 15"/>
          </svg>
          <div><strong>No Scanned Links Yet</strong></div>
          <div style="font-size:12px;">Scanned QR codes and their verdicts will be saved here.</div>
        </div>`;
      return;
    }

    historyListEl.innerHTML = historyItems
      .map((item) => {
        const dt = new Date(item.timestamp).toLocaleString();
        return `
          <div class="history-item">
            <div class="item-top">
              <span class="verdict-pill ${item.threatLevel}">${escapeHtml(item.threatLabel)}</span>
              <span class="item-time">${escapeHtml(dt)}</span>
            </div>
            <div class="item-url">${escapeHtml(item.url)}</div>
            <div class="item-meta">${escapeHtml(item.summary)}</div>
            <div class="item-actions">
              <button class="chip-btn" data-action="reinspect" data-url="${escapeHtml(item.url)}">Re-Inspect</button>
              <button class="chip-btn" data-action="delete" data-id="${escapeHtml(item.id)}" style="color:#f87171;">Delete</button>
            </div>
          </div>`;
      })
      .join('');
  }

  // Render Test Sandbox
  function renderSandbox() {
    sandboxListEl.innerHTML = TEST_SCENARIOS.map(
      (s, idx) => `
      <div class="scenario-item">
        <div class="item-top">
          <strong style="font-size:14px;">${escapeHtml(s.title)}</strong>
          <span class="verdict-pill ${s.expected}">${escapeHtml(s.expectedLabel)}</span>
        </div>
        <div class="item-meta">${escapeHtml(s.desc)}</div>
        <div class="item-url" style="color:var(--primary);font-size:12px;">${escapeHtml(s.url)}</div>
        <div class="item-actions">
          <button class="chip-btn" data-sandbox-test="${idx}">Run Heuristic Check</button>
          <button class="chip-btn" data-sandbox-qr="${idx}">Show QR Code</button>
        </div>
      </div>`
    ).join('');
  }

  async function showQrPreview(scenario) {
    activePreviewUrl = scenario.url;
    qrPreviewTitle.textContent = scenario.title;
    qrPreviewUrl.textContent = scenario.url;
    qrPreviewImg.src = '';
    qrPreviewModal.classList.add('open');

    try {
      const resp = await fetch(`/api/qrcode?text=${encodeURIComponent(scenario.url)}`);
      const data = await resp.json();
      if (data.dataUrl) {
        qrPreviewImg.src = data.dataUrl;
        btnQrDownload.href = data.dataUrl;
      }
    } catch (_) {
      showToast('Failed to generate test QR image');
    }
  }

  // Event Listeners
  btnStartCamera.addEventListener('click', startCamera);
  btnToggleCamera.addEventListener('click', () => {
    if (scanningActive) {
      stopCamera(true);
    } else {
      startCamera();
    }
  });
  btnTorch.addEventListener('click', toggleTorch);
  btnSwitchCamera.addEventListener('click', switchCamera);
  btnUploadImage.addEventListener('click', () => fileInput.click());

  btnQuickCheck.addEventListener('click', () => {
    const val = quickUrlInput.value.trim();
    if (!val) {
      showToast('Enter a URL or text to inspect');
      return;
    }
    handleDetectedContent(val, true);
    quickUrlInput.value = '';
  });

  quickUrlInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') btnQuickCheck.click();
  });

  btnCopyUrl.addEventListener('click', async () => {
    if (!currentAnalysis) return;
    try {
      await navigator.clipboard.writeText(currentAnalysis.parsedUrl);
      showToast('Link copied to clipboard');
    } catch (_) {
      showToast('Copy failed');
    }
  });

  btnDontOpen.addEventListener('click', () => {
    showToast('Blocked — stayed safe');
    closeVerdictModal();
  });

  btnOpenAnyway.addEventListener('click', () => {
    if (!currentAnalysis) return;
    const targetUrl = currentAnalysis.parsedUrl;
    closeVerdictModal();
    window.open(targetUrl, '_blank', 'noopener,noreferrer');
  });

  historyListEl.addEventListener('click', (e) => {
    const btn = e.target.closest('button[data-action]');
    if (!btn) return;
    const action = btn.dataset.action;
    if (action === 'reinspect') {
      handleDetectedContent(btn.dataset.url, false);
    } else if (action === 'delete') {
      historyItems = historyItems.filter((i) => i.id !== btn.dataset.id);
      saveHistory();
      showToast('Removed from history');
    }
  });

  btnClearHistory.addEventListener('click', () => {
    if (confirm('Clear all scan history logs?')) {
      historyItems = [];
      saveHistory();
      showToast('History cleared');
    }
  });

  sandboxListEl.addEventListener('click', (e) => {
    const testBtn = e.target.closest('button[data-sandbox-test]');
    if (testBtn) {
      const scenario = TEST_SCENARIOS[Number(testBtn.dataset.sandboxTest)];
      if (scenario) handleDetectedContent(scenario.url, true);
      return;
    }
    const qrBtn = e.target.closest('button[data-sandbox-qr]');
    if (qrBtn) {
      const scenario = TEST_SCENARIOS[Number(qrBtn.dataset.sandboxQr)];
      if (scenario) showQrPreview(scenario);
    }
  });

  btnQrTestNow.addEventListener('click', () => {
    qrPreviewModal.classList.remove('open');
    if (activePreviewUrl) handleDetectedContent(activePreviewUrl, true);
  });

  btnQrClose.addEventListener('click', () => {
    qrPreviewModal.classList.remove('open');
  });

  // Init
  renderSandbox();
  renderHistory();

  // Auto-start live camera immediately when opened on HTTPS or localhost
  if (window.isSecureContext && navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
    startCamera(true);
  }
})();
