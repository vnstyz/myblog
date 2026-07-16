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
        const tip = document.querySelector('.like-tip');

        const showTip = (msg, isError) => {
            if (!tip) return;
            tip.textContent = msg;
            tip.className = 'like-tip' + (isError ? ' error' : ' success');
            // 3 秒后自动隐藏
            clearTimeout(tip._timer);
            tip._timer = setTimeout(() => {
                tip.textContent = '';
                tip.className = 'like-tip';
            }, 3000);
        };

        likeBtn.addEventListener('click', async function () {
            // 请求进行中防重复点击
            if (this.classList.contains('loading')) return;
            this.classList.add('loading');

            const articleId = this.dataset.articleId;
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

            try {
                const res = await fetch(`/api/like/${articleId}`, {
                    method: 'POST',
                    headers: Object.assign(
                        { 'Content-Type': 'application/json' },
                        csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {}
                    )
                });
                const data = await res.json();

                if (res.ok && data.success) {
                    this.classList.add('liked');
                    const icon = this.querySelector('i');
                    if (icon) {
                        icon.style.transform = 'scale(1.4)';
                        setTimeout(() => icon.style.transform = 'scale(1)', 200);
                    }
                    const countSpan = this.querySelector('.like-count');
                    if (countSpan) countSpan.textContent = data.likeCount;
                    showTip('点赞成功，感谢支持！', false);
                } else {
                    // 冷却中(429)或文章不存在(404)
                    this.classList.add('liked');
                    showTip(data.message || '操作失败，请稍后再试', true);
                }
            } catch (e) {
                showTip('网络异常，请稍后再试', true);
            } finally {
                this.classList.remove('loading');
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

    /* ===== 评论区 ===== */
    const commentSection = document.querySelector('.comment-section');
    if (commentSection) {
        const articleId = commentSection.dataset.articleId;
        const commentList = document.getElementById('commentList');
        const commentEmpty = document.getElementById('commentEmpty');
        const commentForm = document.getElementById('commentForm');
        const commentInput = document.getElementById('commentInput');
        const charCount = document.getElementById('commentCharCount');
        const commentTip = commentSection.querySelector('.comment-tip');
        const countBadge = commentSection.querySelector('.comment-count-badge');

        const showCommentTip = (msg, isError) => {
            if (!commentTip) return;
            commentTip.textContent = msg;
            commentTip.className = 'comment-tip' + (isError ? ' error' : ' success');
            clearTimeout(commentTip._timer);
            commentTip._timer = setTimeout(() => {
                commentTip.textContent = '';
                commentTip.className = 'comment-tip';
            }, 3000);
        };

        const escapeHtml = (str) => {
            const div = document.createElement('div');
            div.textContent = str == null ? '' : str;
            return div.innerHTML;
        };

        const formatRelativeTime = (iso) => {
            const then = new Date(iso);
            if (isNaN(then.getTime())) return '';
            const diff = (Date.now() - then.getTime()) / 1000;
            if (diff < 60) return '刚刚';
            if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前';
            if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前';
            if (diff < 2592000) return Math.floor(diff / 86400) + ' 天前';
            const y = then.getFullYear();
            const m = String(then.getMonth() + 1).padStart(2, '0');
            const d = String(then.getDate()).padStart(2, '0');
            return `${y}-${m}-${d}`;
        };

        const renderComment = (c, prepend) => {
            const li = document.createElement('li');
            li.className = 'comment-item';
            const avatarChar = (c.authorName || '访').charAt(0);
            li.innerHTML =
                '<div class="comment-avatar">' + escapeHtml(avatarChar) + '</div>' +
                '<div class="comment-body">' +
                    '<div class="comment-meta">' +
                        '<span class="comment-author">' + escapeHtml(c.authorName) + '</span>' +
                        '<span class="comment-time">' + formatRelativeTime(c.createdTime) + '</span>' +
                    '</div>' +
                    '<div class="comment-content">' + escapeHtml(c.content) + '</div>' +
                '</div>';
            if (prepend && commentList.firstChild) {
                commentList.insertBefore(li, commentList.firstChild);
            } else {
                commentList.appendChild(li);
            }
        };

        const loadComments = async () => {
            try {
                const res = await fetch(`/api/comments/${articleId}`);
                if (!res.ok) return;
                const comments = await res.json();
                commentList.innerHTML = '';
                if (Array.isArray(comments) && comments.length) {
                    comments.forEach(c => renderComment(c, false));
                    if (commentEmpty) commentEmpty.style.display = 'none';
                } else if (commentEmpty) {
                    commentEmpty.style.display = 'block';
                }
            } catch (e) {
                // 评论加载失败不影响文章阅读，静默处理
            }
        };

        if (commentInput && charCount) {
            commentInput.addEventListener('input', () => {
                charCount.textContent = commentInput.value.length;
            });
        }

        if (commentForm) {
            commentForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const content = commentInput.value.trim();
                if (!content) {
                    showCommentTip('评论内容不能为空', true);
                    return;
                }
                const submitBtn = commentForm.querySelector('.comment-submit');
                if (submitBtn && submitBtn.classList.contains('loading')) return;
                if (submitBtn) submitBtn.classList.add('loading');

                const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
                const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

                try {
                    const res = await fetch(`/api/comments/${articleId}`, {
                        method: 'POST',
                        headers: Object.assign(
                            { 'Content-Type': 'application/json' },
                            csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {}
                        ),
                        body: JSON.stringify({ content })
                    });
                    const data = await res.json();

                    if (res.ok && data.success) {
                        commentInput.value = '';
                        if (charCount) charCount.textContent = '0';
                        if (data.comment) renderComment(data.comment, true);
                        if (commentEmpty) commentEmpty.style.display = 'none';
                        if (countBadge && data.commentCount != null) {
                            countBadge.textContent = data.commentCount;
                        }
                        showCommentTip('评论成功，感谢参与！', false);
                    } else {
                        showCommentTip(data.message || '评论失败，请稍后再试', true);
                    }
                } catch (err) {
                    showCommentTip('网络异常，请稍后再试', true);
                } finally {
                    if (submitBtn) submitBtn.classList.remove('loading');
                }
            });
        }

        loadComments();
    }

});
