/* =============================================
   app.js — Shooting Stars + Mount Fuji
   ============================================= */

// ==========================================
// 1. NAV SCROLL EFFECT
// ==========================================
const navbar = document.getElementById('navbar');
window.addEventListener('scroll', () => {
    navbar.classList.toggle('scrolled', window.scrollY > 40);
});

// ==========================================
// 2. THEME TOGGLE
// ==========================================
const html = document.documentElement;
const themeToggle = document.getElementById('theme-toggle');

const savedTheme = localStorage.getItem('theme') || 'dark';
if (savedTheme === 'light') html.classList.add('light');

function isLight() { return html.classList.contains('light'); }

themeToggle.addEventListener('click', () => {
    html.classList.toggle('light');
    localStorage.setItem('theme', isLight() ? 'light' : 'dark');
});

// ==========================================
// 3. HAMBURGER MOBILE MENU
// ==========================================
const hamburger = document.getElementById('hamburger');
const mobileMenu = document.getElementById('mobile-menu');
hamburger.addEventListener('click', () => mobileMenu.classList.toggle('open'));
function closeMobileMenu() { mobileMenu.classList.remove('open'); }

// ==========================================
// 4. CONTACT FORM
// ==========================================
function handleSubmit(e) {
    e.preventDefault();
    const btn = document.getElementById('submit-btn');
    const text = document.getElementById('btn-text');
    const succ = document.getElementById('form-success');
    btn.disabled = true;
    text.textContent = 'Sending...';
    setTimeout(() => {
        text.textContent = 'Send Message';
        btn.disabled = false;
        succ.style.display = 'block';
        document.getElementById('contact-form').reset();
        setTimeout(() => { succ.style.display = 'none'; }, 5000);
    }, 1500);
}

// ==========================================
// 5. CARD SCROLL ANIMATIONS
// ==========================================
const cardObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            const delay = entry.target.dataset.delay || 0;
            setTimeout(() => entry.target.classList.add('visible'), Number(delay));
        }
    });
}, { threshold: 0.15 });

document.querySelectorAll('.service-card').forEach(card => {
    card.style.opacity = '0';
    card.style.transform = 'translateY(28px)';
    card.style.transition = 'opacity 0.8s ease, transform 0.8s ease';
    cardObserver.observe(card);
});

// ==========================================
// 6. MOUNT FUJI + SHOOTING STARS CANVAS
// ==========================================
(function initScene() {
    const canvas = document.getElementById('mountain-canvas');
    const ctx = canvas.getContext('2d');
    let W, H, t = 0;

    function resize() {
        W = canvas.width = window.innerWidth;
        H = canvas.height = window.innerHeight;
    }
    window.addEventListener('resize', resize);
    resize();

    // ---- SHOOTING STARS ----
    const MAX_STARS = 8;
    const shooters = [];

    function makeShooter() {
        const angle = Math.PI / 5 + Math.random() * Math.PI / 8; // ~36–58° downward diagonal
        const speed = 8 + Math.random() * 10;
        return {
            x: Math.random() * W * 0.85,
            y: Math.random() * H * 0.45,
            vx: Math.cos(angle) * speed,
            vy: Math.sin(angle) * speed,
            len: 80 + Math.random() * 160,  // trail length
            alpha: 0,
            life: 0,
            maxLife: 55 + Math.random() * 40,
            w: 1 + Math.random() * 1.5,   // trail width
            dead: false,
        };
    }

    // Stagger initial population
    for (let i = 0; i < MAX_STARS; i++) {
        const s = makeShooter();
        s.life = Math.random() * s.maxLife; // pre-age so they don't all appear at once
        shooters.push(s);
    }

    let frameCount = 0;

    function updateShooters() {
        frameCount++;
        // Spawn new one every ~2–4 seconds randomly
        if (frameCount % (70 + Math.floor(Math.random() * 80)) === 0) {
            const dead = shooters.findIndex(s => s.dead);
            if (dead !== -1) shooters[dead] = makeShooter();
        }

        shooters.forEach(s => {
            if (s.dead) return;
            s.life++;
            const progress = s.life / s.maxLife;
            // Fade in quickly, hold, then fade out
            if (progress < 0.2) {
                s.alpha = progress / 0.2;
            } else if (progress < 0.75) {
                s.alpha = 1;
            } else {
                s.alpha = 1 - (progress - 0.75) / 0.25;
            }
            s.x += s.vx;
            s.y += s.vy;
            if (s.life >= s.maxLife) { s.dead = true; }
        });
    }

    function drawShooters(light) {
        shooters.forEach(s => {
            if (s.dead || s.alpha <= 0) return;
            const tailX = s.x - s.vx / 8 * s.len / 14;
            const tailY = s.y - s.vy / 8 * s.len / 14;
            const grad = ctx.createLinearGradient(tailX, tailY, s.x, s.y);
            const col = light ? '180,150,100' : '255,240,200';
            grad.addColorStop(0, `rgba(${col},0)`);
            grad.addColorStop(0.6, `rgba(${col},${s.alpha * 0.45})`);
            grad.addColorStop(1, `rgba(${col},${s.alpha})`);

            ctx.beginPath();
            ctx.moveTo(tailX, tailY);
            ctx.lineTo(s.x, s.y);
            ctx.strokeStyle = grad;
            ctx.lineWidth = s.w;
            ctx.lineCap = 'round';
            ctx.globalAlpha = 1;
            ctx.stroke();

            // Bright leading tip
            ctx.beginPath();
            ctx.arc(s.x, s.y, s.w * 1.4, 0, Math.PI * 2);
            ctx.fillStyle = light
                ? `rgba(255,210,140,${s.alpha})`
                : `rgba(255,252,220,${s.alpha})`;
            ctx.fill();
        });
    }

    // ---- SKY ----
    function drawDarkSky() {
        const grad = ctx.createLinearGradient(0, 0, 0, H);
        grad.addColorStop(0, '#010208');
        grad.addColorStop(0.4, '#050610');
        grad.addColorStop(0.7, '#0c0818');
        grad.addColorStop(1, '#18102a');
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, W, H);
    }

    function drawDaySky() {
        const grad = ctx.createLinearGradient(0, 0, 0, H);
        grad.addColorStop(0, '#3a6ea0');
        grad.addColorStop(0.4, '#6aa0cc');
        grad.addColorStop(0.7, '#a0cce0');
        grad.addColorStop(1, '#c8e4f0');
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, W, H);
    }

    // ---- MOUNT FUJI ----
    function drawFuji(light) {
        const baseY = H * 0.72;
        const peakX = W * 0.5;
        const peakY = H * 0.22;
        const baseW = W * 0.88;

        // Soft depth haze
        const hazeC = light ? '170,200,220' : '60,30,80';
        const haze = ctx.createLinearGradient(peakX - baseW * 0.8, baseY, peakX + baseW * 0.8, baseY);
        haze.addColorStop(0, `rgba(${hazeC},0)`);
        haze.addColorStop(0.5, `rgba(${hazeC},0.28)`);
        haze.addColorStop(1, `rgba(${hazeC},0)`);
        ctx.fillStyle = haze;
        ctx.fillRect(0, H * 0.52, W, H * 0.22);

        // Mountain silhouette
        ctx.beginPath();
        ctx.moveTo(peakX - baseW / 2, baseY);
        ctx.bezierCurveTo(
            peakX - baseW * 0.36, baseY * 0.73,
            peakX - baseW * 0.21, peakY + (baseY - peakY) * 0.36,
            peakX - baseW * 0.055, peakY + 28
        );
        ctx.lineTo(peakX, peakY);
        ctx.lineTo(peakX + baseW * 0.055, peakY + 28);
        ctx.bezierCurveTo(
            peakX + baseW * 0.21, peakY + (baseY - peakY) * 0.36,
            peakX + baseW * 0.36, baseY * 0.73,
            peakX + baseW / 2, baseY
        );
        ctx.closePath();

        const mGrad = ctx.createLinearGradient(peakX, peakY, peakX, baseY);
        if (light) {
            mGrad.addColorStop(0, 'rgba(80,100,130,1)');
            mGrad.addColorStop(0.3, 'rgba(110,130,160,0.95)');
            mGrad.addColorStop(0.7, 'rgba(140,160,188,0.88)');
            mGrad.addColorStop(1, 'rgba(160,180,205,0.72)');
        } else {
            mGrad.addColorStop(0, 'rgba(22,14,38,1)');
            mGrad.addColorStop(0.2, 'rgba(38,26,62,1)');
            mGrad.addColorStop(0.55, 'rgba(50,34,76,0.94)');
            mGrad.addColorStop(1, 'rgba(32,20,54,0.72)');
        }
        ctx.fillStyle = mGrad;
        ctx.fill();

        // Left-face shadow
        ctx.beginPath();
        ctx.moveTo(peakX, peakY);
        ctx.lineTo(peakX - baseW * 0.055, peakY + 28);
        ctx.bezierCurveTo(
            peakX - baseW * 0.21, peakY + (baseY - peakY) * 0.36,
            peakX - baseW * 0.36, baseY * 0.73,
            peakX - baseW / 2, baseY
        );
        ctx.lineTo(peakX - baseW * 0.12, baseY);
        ctx.closePath();
        ctx.fillStyle = light ? 'rgba(50,70,100,0.15)' : 'rgba(10,6,22,0.28)';
        ctx.fill();

        // Snow cap
        const sH = (baseY - peakY) * 0.18;
        ctx.beginPath();
        ctx.moveTo(peakX, peakY);
        ctx.lineTo(peakX - baseW * 0.048, peakY + sH * 0.68);
        ctx.bezierCurveTo(
            peakX - baseW * 0.042, peakY + sH,
            peakX - baseW * 0.02, peakY + sH * 0.78,
            peakX, peakY + sH * 1.4
        );
        ctx.bezierCurveTo(
            peakX + baseW * 0.02, peakY + sH * 0.78,
            peakX + baseW * 0.042, peakY + sH,
            peakX + baseW * 0.048, peakY + sH * 0.68
        );
        ctx.closePath();
        const sg = ctx.createLinearGradient(peakX, peakY, peakX, peakY + sH * 1.4);
        sg.addColorStop(0, 'rgba(255,253,250,1)');
        sg.addColorStop(0.55, 'rgba(238,240,250,0.97)');
        sg.addColorStop(1, 'rgba(210,218,238,0.72)');
        ctx.fillStyle = sg;
        ctx.fill();

        // Base glow
        const baseGlow = ctx.createLinearGradient(0, baseY - 10, 0, baseY + 40);
        baseGlow.addColorStop(0, light ? 'rgba(160,200,230,0.18)' : 'rgba(80,50,130,0.12)');
        baseGlow.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.fillStyle = baseGlow;
        ctx.fillRect(0, baseY - 10, W, 50);
    }

    // ---- GROUND VEIL ----
    function drawVeil(light) {
        const g = ctx.createLinearGradient(0, H * 0.76, 0, H);
        if (light) {
            g.addColorStop(0, 'rgba(190,215,230,0)');
            g.addColorStop(1, 'rgba(200,225,240,0.6)');
        } else {
            g.addColorStop(0, 'rgba(8,5,18,0)');
            g.addColorStop(1, 'rgba(8,5,18,0.88)');
        }
        ctx.fillStyle = g;
        ctx.fillRect(0, H * 0.76, W, H * 0.24);
    }

    function drawFrame() {
        t += 0.012;
        const light = isLight();
        ctx.clearRect(0, 0, W, H);

        if (light) drawDaySky();
        else drawDarkSky();

        updateShooters();
        drawShooters(light);
        drawFuji(light);
        drawVeil(light);
        requestAnimationFrame(drawFrame);
    }
    drawFrame();
})();

// ==========================================
// 7. SAKURA CANVAS — hidden (cleared)
// ==========================================
// Cherry blossom rain removed per user request.
// The sakura canvas element is kept in the DOM but
// we simply never draw on it.
(function clearSakura() {
    const canvas = document.getElementById('sakura-canvas');
    if (canvas) {
        canvas.width = 1;
        canvas.height = 1;
    }
})();
