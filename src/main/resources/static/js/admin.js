/**
 * 后台管理 - 文章 CRUD
 */
(function () {
    const API_BASE = '/admin/api/articles';
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || '';

    const articleModal = document.getElementById('articleModal');
    const deleteModal = document.getElementById('deleteModal');
    const articleForm = document.getElementById('articleForm');
    const articlesTableBody = document.getElementById('articlesTableBody');

    let currentDeleteId = null;
    let easyMDE = null;

    function getHeaders() {
        const headers = {
            'Content-Type': 'application/json'
        };
        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }
        return headers;
    }

    async function request(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            headers: {
                ...getHeaders(),
                ...(options.headers || {})
            }
        });
        if (!response.ok) {
            const text = await response.text().catch(() => '');
            throw new Error(text || `请求失败: ${response.status}`);
        }
        return response.status === 204 ? null : response.json();
    }

    function openArticleModal(article) {
        document.getElementById('modalTitle').textContent = article ? '编辑文章' : '新建文章';
        document.getElementById('articleId').value = article?.id || '';
        document.getElementById('articleTitle').value = article?.title || '';
        document.getElementById('articleStatus').value = article?.status ?? 0;
        document.getElementById('articleCategory').value = article?.categoryId ?? 1;
        document.getElementById('articleSummary').value = article?.summary || '';
        if (easyMDE) {
            easyMDE.value(article?.content || '');
        }
        articleModal.classList.add('active');
    }

    function closeArticleModal() {
        articleModal.classList.remove('active');
        articleForm.reset();
        document.getElementById('articleId').value = '';
        if (easyMDE) {
            easyMDE.value('');
        }
    }

    function openDeleteModal(article) {
        currentDeleteId = article.id;
        document.getElementById('deleteArticleTitle').textContent = article.title || '未命名文章';
        deleteModal.classList.add('active');
    }

    function closeDeleteModal() {
        deleteModal.classList.remove('active');
        currentDeleteId = null;
    }

    function formatDateTime(value) {
        if (!value) return '-';
        const date = new Date(value);
        return isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN');
    }

    function createArticleRow(article) {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><a href="/article/${article.id}">${escapeHtml(article.title || '无标题')}</a></td>
            <td><span class="admin-status ${article.status === 1 ? 'published' : 'draft'}">${article.status === 1 ? '已发布' : '草稿'}</span></td>
            <td>${article.viewCount || 0}</td>
            <td>${article.likeCount || 0}</td>
            <td>${formatDateTime(article.createdTime)}</td>
            <td class="admin-actions">
                <button class="btn-icon btn-edit" data-id="${article.id}" title="编辑"><i class="ri-edit-line"></i></button>
                <button class="btn-icon btn-delete" data-id="${article.id}" data-title="${escapeHtml(article.title || '')}" title="删除"><i class="ri-delete-bin-line"></i></button>
            </td>
        `;
        return tr;
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    async function loadArticles() {
        try {
            const articles = await request(API_BASE);
            if (!articlesTableBody) return;
            articlesTableBody.innerHTML = '';
            if (!articles || articles.length === 0) {
                const tr = document.createElement('tr');
                tr.id = 'emptyRow';
                tr.innerHTML = '<td colspan="6" class="admin-empty-cell">暂无文章</td>';
                articlesTableBody.appendChild(tr);
                return;
            }
            articles.forEach(article => {
                articlesTableBody.appendChild(createArticleRow(article));
            });
        } catch (err) {
            alert('加载文章失败: ' + err.message);
        }
    }

    async function saveArticle(e) {
        e.preventDefault();
        const id = document.getElementById('articleId').value;
        const content = easyMDE ? easyMDE.value() : '';
        if (!content.trim()) {
            alert('请输入文章内容');
            return;
        }
        const article = {
            title: document.getElementById('articleTitle').value,
            status: parseInt(document.getElementById('articleStatus').value, 10),
            categoryId: parseInt(document.getElementById('articleCategory').value, 10),
            summary: document.getElementById('articleSummary').value,
            content: easyMDE ? easyMDE.value() : ''
        };

        try {
            const method = id ? 'PUT' : 'POST';
            const url = id ? `${API_BASE}/${id}` : API_BASE;
            await request(url, {
                method,
                body: JSON.stringify(article)
            });
            closeArticleModal();
            await loadArticles();
        } catch (err) {
            alert('保存文章失败: ' + err.message);
        }
    }

    async function confirmDelete() {
        if (!currentDeleteId) return;
        try {
            await request(`${API_BASE}/${currentDeleteId}`, { method: 'DELETE' });
            closeDeleteModal();
            await loadArticles();
        } catch (err) {
            alert('删除文章失败: ' + err.message);
        }
    }

    function init() {
        if (!articlesTableBody) return;

        easyMDE = new EasyMDE({
            element: document.getElementById('articleContentMarkdown'),
            spellChecker: false,
            status: false,
            minHeight: '280px',
            toolbar: [
                'bold', 'italic', 'heading', '|',
                'quote', 'unordered-list', 'ordered-list', '|',
                'link', 'image', 'code', 'table', '|',
                'preview', 'side-by-side', 'fullscreen', '|',
                'guide'
            ],
            placeholder: '请输入 Markdown 文章内容...'
        });

        document.getElementById('btnAddArticle')?.addEventListener('click', () => openArticleModal(null));
        document.getElementById('modalClose')?.addEventListener('click', closeArticleModal);
        document.getElementById('btnCancel')?.addEventListener('click', closeArticleModal);
        articleForm?.addEventListener('submit', saveArticle);

        document.getElementById('deleteModalClose')?.addEventListener('click', closeDeleteModal);
        document.getElementById('btnCancelDelete')?.addEventListener('click', closeDeleteModal);
        document.getElementById('btnConfirmDelete')?.addEventListener('click', confirmDelete);

        articlesTableBody.addEventListener('click', async (e) => {
            const editBtn = e.target.closest('.btn-edit');
            const deleteBtn = e.target.closest('.btn-delete');
            if (editBtn) {
                const id = editBtn.dataset.id;
                try {
                    const article = await request(`${API_BASE}/${id}`);
                    openArticleModal(article);
                } catch (err) {
                    alert('加载文章失败: ' + err.message);
                }
            } else if (deleteBtn) {
                const id = deleteBtn.dataset.id;
                const title = deleteBtn.dataset.title || '';
                openDeleteModal({ id, title });
            }
        });
    }

    init();
})();
