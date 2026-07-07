/**
 * 现代个人博客 - 交互脚本
 */

document.addEventListener('DOMContentLoaded', () => {

    /* ===== 导航栏滚动效果 ===== */
    const navbar = document.querySelector('.navbar');
    if (navbar) {
        window.addEventListener('scroll', () => {
            navbar.classList.toggle('scrolled', window.scrollY > 50);
        });
    }

    /* ===== 移动端菜单切换 ===== */
    const navToggle = document.querySelector('.nav-toggle');
    const navLinks = document.querySelector('.nav-links');
    if (navToggle && navLinks) {
        navToggle.addEventListener('click', () => {
            navLinks.classList.toggle('open');
        });
        document.addEventListener('click', (e) => {
            if (!e.target.closest('.navbar')) {
                navLinks.classList.remove('open');
            }
        });
    }

    /* ===== 滚动渐显动画 ===== */
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
            }
        });
    }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' });

    document.querySelectorAll('.fade-up').forEach(el => observer.observe(el));

    /* ===== 技能条动画 ===== */
    const skillObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const bar = entry.target;
                bar.style.width = bar.dataset.width || '0%';
            }
        });
    }, { threshold: 0.5 });

    document.querySelectorAll('.skill-bar-fill').forEach(bar => {
        const target = bar.style.width;
        bar.style.width = '0%';
        bar.dataset.width = target;
        skillObserver.observe(bar);
    });

    /* ===== 星空背景 ===== */
    const starsContainer = document.querySelector('.stars-container');
    if (starsContainer) {
        for (let i = 0; i < 80; i++) {
            const star = document.createElement('div');
            star.classList.add('star');
            star.style.left = Math.random() * 100 + '%';
            star.style.top = Math.random() * 100 + '%';
            star.style.width = (Math.random() * 2 + 0.5) + 'px';
            star.style.height = star.style.width;
            star.style.setProperty('--duration', (Math.random() * 3 + 2) + 's');
            star.style.setProperty('--delay', (Math.random() * 3) + 's');
            starsContainer.appendChild(star);
        }
    }

    /* ===== 点赞按钮 ===== */
    const likeBtn = document.querySelector('.like-btn');
    if (likeBtn) {
        likeBtn.addEventListener('click', function () {
            this.classList.toggle('liked');

            // 心跳动画
            const icon = this.querySelector('i');
            if (icon) {
                icon.style.transform = 'scale(1.4)';
                setTimeout(() => icon.style.transform = 'scale(1)', 200);
            }

            // 更新计数
            const countSpan = this.querySelector('.like-count');
            if (countSpan) {
                const count = parseInt(countSpan.textContent) || 0;
                countSpan.textContent = this.classList.contains('liked') ? count + 1 : count;
            }
        });
    }

    /* ===== 平滑滚动 ===== */
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                e.preventDefault();
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });

});
