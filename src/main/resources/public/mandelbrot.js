/**
 * Mandelbrot Fractal Explorer - JavaScript
 *
 * Interactive fractal viewer with zoom, pan, and multi-touch support.
 * Features:
 * - Click and drag to zoom into a region
 * - Shift+drag or right-click drag to pan
 * - Two-finger pinch zoom and pan on mobile
 * - Undo/redo functionality
 * - Multiple color palettes
 * - Responsive design for mobile and desktop
 */

// ================================================
// DOM Element References
// ================================================

const fractalImage = document.getElementById('fractalImage');
const container = document.getElementById('fractal-container');
const selectionBox = document.getElementById('selection-box');
const resetButton = document.getElementById('resetButton');
const undoButton = document.getElementById('undoButton');
const paletteSelect = document.getElementById('paletteSelect');
const loadingIndicator = document.getElementById('loading-indicator');
const panModeButton = document.getElementById('panModeButton');
const helpButton = document.getElementById('helpButton');
const helpOverlay = document.getElementById('help-overlay');

const palettes = Array.from(paletteSelect.options).map(o => o.value);
const defaultPalette = palettes[0];

// ================================================
// Loading Indicator Functions
// ================================================

function showLoading() {
    loadingIndicator.classList.add('active');
}

function hideLoading() {
    loadingIndicator.classList.remove('active');
}

// ================================================
// Image Size Calculation
// ================================================

/**
 * Calculate optimal image size based on viewport and device pixel ratio
 */
function getOptimalImageSize() {
    const rect = container.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;

    // Use actual container dimensions (not square)
    let width = Math.max(400, Math.floor(rect.width * Math.min(dpr, 2)));
    let height = Math.max(400, Math.floor(rect.height * Math.min(dpr, 2)));

    // Respect max boundaries
    const maxDimension = 2000;
    if (width > maxDimension || height > maxDimension) {
        const scale = Math.min(maxDimension / width, maxDimension / height);
        width = Math.floor(width * scale);
        height = Math.floor(height * scale);
    }

    return { width, height };
}

let { width, height } = getOptimalImageSize();

// ================================================
// Application State
// ================================================

let state = {
    centerX: -0.5,
    centerY: 0.0,
    zoom: 1.0,
    palette: defaultPalette
};

let history = [];

// Interaction state
let isSelecting = false;
let isPanning = false;
let panModeEnabled = false;
let startX, startY;
let panStartCenterX, panStartCenterY;

// Pinch gesture state
let isPinching = false;
let isTwoFingerPan = false;
let lastPinchDistance = 0;
let pinchStartZoom = 1.0;
let pinchCenterX = 0;
let pinchCenterY = 0;
let lastTwoFingerCenter = null;
let twoFingerPanStartCenterX = 0;
let twoFingerPanStartCenterY = 0;

// ================================================
// Cookie Management
// ================================================

function setCookie(name, value, days) {
    let expires = "";
    if (days) {
        const date = new Date();
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
        expires = "; expires=" + date.toUTCString();
    }
    document.cookie = name + "=" + (value || "") + expires + "; path=/";
}

function getCookie(name) {
    const nameEQ = name + "=";
    const ca = document.cookie.split(';');
    for (let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) === ' ') c = c.substring(1, c.length);
        if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
    }
    return null;
}

// ================================================
// Image Update Function
// ================================================

function updateImage() {
    const { centerX, centerY, zoom, palette } = state;
    console.log(`centerX=${centerX}, centerY=${centerY}, zoom=${zoom}, palette=${palette}`);

    // Recalculate size in case viewport changed (rotation, resize)
    const size = getOptimalImageSize();
    width = size.width;
    height = size.height;

    const imgSrc = `/mandelbrot/image?width=${width}&height=${height}&centerX=${centerX}&centerY=${centerY}&zoom=${zoom}&palette=${palette}`
    console.log(`setting image src to ${imgSrc}`)

    showLoading();

    fractalImage.src = imgSrc;
    setCookie("mandelbrotState", JSON.stringify(state), 7);
}

// Hide loading when image loads
fractalImage.addEventListener('load', hideLoading);
fractalImage.addEventListener('error', () => {
    hideLoading();
    console.error('Failed to load fractal image');
});

// ================================================
// Reset and Undo Functions
// ================================================

function resetState() {
    let palette = defaultPalette;
    const savedState = getCookie("mandelbrotState");
    if (savedState) {
        const parsed = JSON.parse(savedState);
        if (parsed && parsed.palette) {
            palette = parsed.palette;
        }
    }
    state = {
        centerX: -0.5,
        centerY: 0.0,
        zoom: 1.0,
        palette: palette
    };
    history = [];
    undoButton.disabled = true;
    paletteSelect.value = palette;
    if (palette === 'dark') {
        document.body.classList.add('dark');
    } else {
        document.body.classList.remove('dark');
    }
    updateImage();
}

function undo() {
    if (history.length > 0) {
        state = history.pop();
        if (state.palette === 'dark') {
            document.body.classList.add('dark');
        } else {
            document.body.classList.remove('dark');
        }
        undoButton.disabled = history.length === 0;
        updateImage();
    }
}

// ================================================
// Utility Functions
// ================================================

/**
 * Throttle function for performance on touchmove
 */
function throttle(func, delay) {
    let lastCall = 0;
    let timeoutId = null;

    return function(...args) {
        const now = Date.now();
        const timeSinceLastCall = now - lastCall;

        if (timeSinceLastCall >= delay) {
            lastCall = now;
            func.apply(this, args);
        } else {
            // Ensure final call happens
            clearTimeout(timeoutId);
            timeoutId = setTimeout(() => {
                lastCall = Date.now();
                func.apply(this, args);
            }, delay - timeSinceLastCall);
        }
    };
}

/**
 * Get distance between two touch points (for pinch gesture)
 */
function getTouchDistance(touch1, touch2) {
    const dx = touch2.clientX - touch1.clientX;
    const dy = touch2.clientY - touch1.clientY;
    return Math.sqrt(dx * dx + dy * dy);
}

/**
 * Get center point between two touches
 */
function getTouchCenter(touch1, touch2) {
    const rect = fractalImage.getBoundingClientRect();
    return {
        x: (touch1.clientX + touch2.clientX) / 2 - rect.left,
        y: (touch1.clientY + touch2.clientY) / 2 - rect.top
    };
}

/**
 * Unified position calculation for both mouse and touch events
 */
function getPointerPos(e) {
    const rect = fractalImage.getBoundingClientRect();

    // Handle touch events
    if (e.touches && e.touches.length > 0) {
        return {
            x: e.touches[0].clientX - rect.left,
            y: e.touches[0].clientY - rect.top
        };
    }

    // Handle mouse events
    return {
        x: e.clientX - rect.left,
        y: e.clientY - rect.top
    };
}

// ================================================
// Pointer Event Handlers
// ================================================

/**
 * Unified start handler for both mouse and touch
 */
function handlePointerStart(e) {
    // Handle two-finger gestures (pinch zoom and two-finger pan)
    if (e.touches && e.touches.length === 2) {
        e.preventDefault();
        isPinching = true;
        isTwoFingerPan = true;
        isSelecting = false;
        isPanning = false;
        selectionBox.style.display = 'none';

        lastPinchDistance = getTouchDistance(e.touches[0], e.touches[1]);
        pinchStartZoom = state.zoom;

        const center = getTouchCenter(e.touches[0], e.touches[1]);
        pinchCenterX = center.x;
        pinchCenterY = center.y;
        lastTwoFingerCenter = center;
        twoFingerPanStartCenterX = state.centerX;
        twoFingerPanStartCenterY = state.centerY;

        return;
    }

    // Handle single touch or mouse
    if (e.touches && e.touches.length > 2) {
        // Ignore 3+ finger gestures
        return;
    }

    const pos = getPointerPos(e);
    startX = pos.x;
    startY = pos.y;

    // Determine if this is a pan or zoom operation
    // Pan if: shift key, right mouse button, or pan mode is enabled
    const isRightClick = e.button === 2;
    const isShiftKey = e.shiftKey;
    const shouldPan = panModeEnabled || isShiftKey || isRightClick;

    if (shouldPan) {
        isPanning = true;
        isSelecting = false;
        isPinching = false;
        panStartCenterX = state.centerX;
        panStartCenterY = state.centerY;
        container.style.cursor = 'grabbing';
        selectionBox.style.display = 'none';
    } else {
        isSelecting = true;
        isPanning = false;
        isPinching = false;
        selectionBox.style.left = `${startX}px`;
        selectionBox.style.top = `${startY}px`;
        selectionBox.style.width = '1px';
        selectionBox.style.height = '0px';
        selectionBox.style.display = 'block';
    }

    // Prevent default only for touch to avoid conflicts
    if (e.touches) {
        e.preventDefault();
    }
}

/**
 * Move handler for both mouse and touch
 */
function handlePointerMove(e) {
    // Handle two-finger gestures (pinch zoom + two-finger pan)
    if ((isPinching || isTwoFingerPan) && e.touches && e.touches.length === 2) {
        e.preventDefault();

        const currentDistance = getTouchDistance(e.touches[0], e.touches[1]);
        const currentCenter = getTouchCenter(e.touches[0], e.touches[1]);

        // Use rendered dimensions for coordinate calculations
        const rect = fractalImage.getBoundingClientRect();
        const renderedWidth = rect.width;
        const renderedHeight = rect.height;

        // Calculate if this is primarily a pinch (distance change) or pan (center movement)
        const distanceChange = Math.abs(currentDistance - lastPinchDistance);
        const centerMovement = Math.sqrt(
            Math.pow(currentCenter.x - lastTwoFingerCenter.x, 2) +
            Math.pow(currentCenter.y - lastTwoFingerCenter.y, 2)
        );

        // Handle pinch zoom if there's significant distance change
        if (distanceChange > 5) {
            const newZoom = Math.max(0.1, Math.min(1e15, pinchStartZoom * (currentDistance / lastPinchDistance)));

            // Calculate coordinate range
            const rangeX = 4.0 / state.zoom;
            const rangeY = 4.0 * (renderedHeight / renderedWidth) / state.zoom;

            const complexX = state.centerX + (pinchCenterX - renderedWidth / 2) * rangeX / renderedWidth;
            const complexY = state.centerY + (pinchCenterY - renderedHeight / 2) * rangeY / renderedHeight;

            // Interpolate center as we zoom
            const zoomRatio = newZoom / state.zoom;
            state.centerX = complexX - (complexX - state.centerX) / zoomRatio;
            state.centerY = complexY - (complexY - state.centerY) / zoomRatio;
            state.zoom = newZoom;
        }

        // Handle two-finger pan if center is moving
        if (centerMovement > 2) {
            const deltaX = currentCenter.x - lastTwoFingerCenter.x;
            const deltaY = currentCenter.y - lastTwoFingerCenter.y;

            const rangeX = 4.0 / state.zoom;
            const rangeY = 4.0 * (renderedHeight / renderedWidth) / state.zoom;

            // Update center based on movement
            state.centerX -= (deltaX * rangeX / renderedWidth);
            state.centerY -= (deltaY * rangeY / renderedHeight);

            // Update tracking center
            lastTwoFingerCenter = currentCenter;
        }

        return;
    }

    // Handle panning
    if (isPanning) {
        const pos = getPointerPos(e);
        const deltaX = pos.x - startX;
        const deltaY = pos.y - startY;

        // Use rendered dimensions for coordinate calculations
        const rect = fractalImage.getBoundingClientRect();
        const renderedWidth = rect.width;
        const renderedHeight = rect.height;

        // Calculate coordinate range accounting for aspect ratio
        const rangeX = 4.0 / state.zoom;
        const rangeY = 4.0 * (renderedHeight / renderedWidth) / state.zoom;

        // Update center based on drag distance (negative because dragging right moves the view left)
        state.centerX = panStartCenterX - (deltaX * rangeX / renderedWidth);
        state.centerY = panStartCenterY - (deltaY * rangeY / renderedHeight);

        if (e.touches) {
            e.preventDefault();
        }
        return;
    }

    // Handle drag selection
    if (!isSelecting) return;

    const pos = getPointerPos(e);
    const currentX = pos.x;
    const currentY = pos.y;

    const deltaX = currentX - startX;
    const deltaY = currentY - startY;

    const boxWidth = Math.abs(deltaX);
    const boxHeight = Math.abs(deltaY);

    const boxX = deltaX < 0 ? pos.x : startX;
    const boxY = deltaY < 0 ? pos.y : startY;

    selectionBox.style.left = `${boxX}px`;
    selectionBox.style.top = `${boxY}px`;
    selectionBox.style.width = `${boxWidth}px`;
    selectionBox.style.height = `${boxHeight}px`;

    if (e.touches) {
        e.preventDefault();
    }
}

/**
 * End handler for both mouse and touch
 */
function handlePointerEnd(e) {
    // Reset cursor
    container.style.cursor = panModeEnabled ? 'grab' : 'crosshair';

    // Handle two-finger gesture end (pinch zoom + pan)
    if (isPinching || isTwoFingerPan) {
        isPinching = false;
        isTwoFingerPan = false;

        // Save to history (state already updated during touchmove)
        history.push({ ...state });
        undoButton.disabled = false;

        // Now update the image with final zoom/pan state
        updateImage();

        if (e.touches !== undefined) {
            e.preventDefault();
        }
        return;
    }

    // Handle panning end
    if (isPanning) {
        isPanning = false;

        const pos = getPointerPos(e);
        const deltaX = Math.abs(pos.x - startX);
        const deltaY = Math.abs(pos.y - startY);

        // Only update if there was actual movement
        if (deltaX > 2 || deltaY > 2) {
            history.push({ ...state });
            undoButton.disabled = false;
            updateImage();
        }

        if (e.touches !== undefined) {
            e.preventDefault();
        }
        return;
    }

    // Handle drag selection end
    if (!isSelecting) return;
    isSelecting = false;
    selectionBox.style.display = 'none';

    const pos = getPointerPos(e);
    const deltaX = pos.x - startX;
    const deltaY = pos.y - startY;

    const boxWidth = Math.abs(deltaX);
    const boxHeight = Math.abs(deltaY);

    if (boxWidth < 10 || boxHeight < 10) {
        // Ignore very small selections (accidental taps)
        return;
    }

    history.push({ ...state });
    undoButton.disabled = false;

    // Use rendered dimensions for coordinate calculations
    const rect = fractalImage.getBoundingClientRect();
    const renderedWidth = rect.width;
    const renderedHeight = rect.height;

    // Calculate zoom based on which dimension requires more zoom
    const zoomFactorX = renderedWidth / boxWidth;
    const zoomFactorY = renderedHeight / boxHeight;
    const newZoom = state.zoom * Math.max(zoomFactorX, zoomFactorY);

    // Calculate coordinate range accounting for aspect ratio
    const rangeX = 4.0 / state.zoom;
    const rangeY = 4.0 * (renderedHeight / renderedWidth) / state.zoom;

    const boxStartX = deltaX < 0 ? startX - boxWidth : startX;
    const boxStartY = deltaY < 0 ? startY - boxHeight : startY;

    // New center in complex plane
    const newCenterX = state.centerX + (boxStartX + boxWidth / 2 - renderedWidth / 2) * rangeX / renderedWidth;
    const newCenterY = state.centerY + (boxStartY + boxHeight / 2 - renderedHeight / 2) * rangeY / renderedHeight;

    state.centerX = newCenterX;
    state.centerY = newCenterY;
    state.zoom = newZoom;

    updateImage();

    if (e.touches !== undefined) {
        e.preventDefault();
    }
}

/**
 * Handle touch cancellation (e.g., system gesture interrupted)
 */
function handlePointerCancel(e) {
    isSelecting = false;
    isPinching = false;
    isPanning = false;
    isTwoFingerPan = false;
    selectionBox.style.display = 'none';
    container.style.cursor = panModeEnabled ? 'grab' : 'crosshair';
}

// ================================================
// Event Listeners - Pointer Events
// ================================================

// Create throttled version for touch (fires max every 16ms ~= 60fps)
const throttledPointerMove = throttle(handlePointerMove, 16);

// Register both mouse and touch event listeners
fractalImage.addEventListener('mousedown', handlePointerStart);
fractalImage.addEventListener('touchstart', handlePointerStart, { passive: false });

document.addEventListener('mousemove', handlePointerMove);  // No throttle for mouse (smooth)
document.addEventListener('touchmove', throttledPointerMove, { passive: false });

document.addEventListener('mouseup', handlePointerEnd);
document.addEventListener('touchend', handlePointerEnd, { passive: false });

document.addEventListener('touchcancel', handlePointerCancel);

// ================================================
// Event Listeners - UI Controls
// ================================================

resetButton.addEventListener('click', resetState);
undoButton.addEventListener('click', undo);

paletteSelect.addEventListener('change', (e) => {
    history.push({ ...state });
    undoButton.disabled = false;
    state.palette = e.target.value;
    if (state.palette === 'dark') {
        document.body.classList.add('dark');
    } else {
        document.body.classList.remove('dark');
    }
    updateImage();
});

// Pan mode toggle
panModeButton.addEventListener('click', () => {
    panModeEnabled = !panModeEnabled;
    if (panModeEnabled) {
        panModeButton.classList.add('active');
        container.style.cursor = 'grab';
    } else {
        panModeButton.classList.remove('active');
        container.style.cursor = 'crosshair';
    }
});

// Help overlay
helpButton.addEventListener('click', () => {
    // Detect if mobile or desktop
    const isMobile = window.matchMedia('(max-width: 768px), (pointer: coarse)').matches;

    // Get the collapsible sections
    const desktopControls = document.getElementById('desktop-controls');
    const mobileControls = document.getElementById('mobile-controls');
    const colorPalettes = document.getElementById('color-palettes');

    // On mobile: show mobile controls, collapse desktop and palettes
    // On desktop: show desktop controls, collapse mobile
    if (isMobile) {
        desktopControls.removeAttribute('open');
        mobileControls.setAttribute('open', 'open');
        colorPalettes.removeAttribute('open');
    } else {
        desktopControls.setAttribute('open', 'open');
        mobileControls.removeAttribute('open');
        colorPalettes.setAttribute('open', 'open');
    }

    helpOverlay.classList.add('visible');
});

helpOverlay.addEventListener('click', (e) => {
    // Close if clicking outside the help content
    if (e.target === helpOverlay) {
        helpOverlay.classList.remove('visible');
    }
});

document.querySelector('.help-close').addEventListener('click', () => {
    helpOverlay.classList.remove('visible');
});

// Prevent context menu on right-click (so we can use it for panning)
container.addEventListener('contextmenu', (e) => {
    e.preventDefault();
});

// ================================================
// Auto-Hide UI
// ================================================

const h1Element = document.querySelector('h1');
const pElement = document.querySelector('p');

function hideUI() {
    // Check if element would be hidden by media queries
    const h1Hidden = window.matchMedia('(max-height: 600px), (max-width: 480px)').matches;
    if (!h1Hidden && h1Element) {
        h1Element.classList.add('hidden');
    }

    const pHidden = window.matchMedia('(max-height: 500px)').matches;
    if (!pHidden && pElement) {
        pElement.classList.add('hidden');
    }
}

// Hide after 10 seconds (one-time, doesn't re-show on interaction)
setTimeout(hideUI, 10000);

// ================================================
// Resize Handlers
// ================================================

// Handle orientation change and resize
let resizeTimeout;
window.addEventListener('resize', () => {
    // Debounce resize events (fires many times during rotation)
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(() => {
        // Recalculate and reload image at new size
        updateImage();
    }, 300);
});

// iOS fires orientationchange separately from resize
window.addEventListener('orientationchange', () => {
    // Wait for viewport to stabilize
    setTimeout(() => {
        updateImage();
    }, 300);
});

// ================================================
// Initial Load
// ================================================

const savedState = getCookie("mandelbrotState");
if (savedState) {
    state = JSON.parse(savedState);
    if (!state.palette || !palettes.includes(state.palette)) {
        state.palette = defaultPalette;
    }
    history = [];
    undoButton.disabled = true;
    paletteSelect.value = state.palette;
    if (state.palette === 'dark') {
        document.body.classList.add('dark');
    } else {
        document.body.classList.remove('dark');
    }
}
updateImage();
