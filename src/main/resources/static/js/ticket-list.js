(function () {
    'use strict';

    function findPageCsrf() {
        var input = Array.prototype.find.call(document.querySelectorAll('input[type="hidden"]'), function (el) {
            return el.name && el.name.toLowerCase().indexOf('csrf') >= 0 && el.value;
        });
        return input ? { name: input.name, value: input.value } : null;
    }

    function appendCsrfToAction(form, actionUrl) {
        var tokenInput = Array.prototype.find.call(form.querySelectorAll('input[type="hidden"]'), function (el) {
            return el.name && el.name.toLowerCase().indexOf('csrf') >= 0 && el.value;
        });
        if (!tokenInput) {
            return actionUrl;
        }
        var sep = actionUrl.indexOf('?') >= 0 ? '&' : '?';
        return actionUrl + sep + encodeURIComponent(tokenInput.name) + '=' + encodeURIComponent(tokenInput.value);
    }

    function escapeHtml(text) {
        if (text == null) {
            return '';
        }
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function openDialog(id) {
        var el = document.getElementById(id);
        if (!el) {
            alert('弹窗未找到，请 Ctrl+F5 强制刷新后重试');
            return null;
        }
        if (typeof el.showModal === 'function') {
            if (!el.open) {
                el.showModal();
            }
        } else {
            el.setAttribute('open', 'open');
            el.classList.add('is-open');
        }
        return el;
    }

    function closeDialog(el) {
        if (!el) {
            return;
        }
        if (typeof el.close === 'function') {
            el.close();
        } else {
            el.removeAttribute('open');
            el.classList.remove('is-open');
        }
    }

    function bindDialogCloseButtons(root) {
        root.querySelectorAll('[data-close-dialog]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var dialog = btn.closest('dialog');
                closeDialog(dialog);
            });
        });
        root.querySelectorAll('dialog.ticket-dialog').forEach(function (dialog) {
            dialog.addEventListener('click', function (e) {
                if (e.target === dialog) {
                    closeDialog(dialog);
                }
            });
        });
    }

    window.openPlanConfirmUpload = function (btn) {
        var ticketId = btn.getAttribute('data-ticket-id');
        var project = btn.getAttribute('data-project') || '';
        var form = document.getElementById('planConfirmUploadForm');
        if (!form) {
            alert('上传表单未找到，请 Ctrl+F5 强制刷新后重试');
            return;
        }
        var label = document.getElementById('planConfirmUploadProject');
        if (label) {
            label.textContent = '项目：' + project;
        }
        form.action = appendCsrfToAction(form, '/tickets/' + ticketId + '/plan-confirmation');
        var fileInput = form.querySelector('input[type="file"]');
        if (fileInput) {
            fileInput.value = '';
        }
        openDialog('planConfirmUploadDialog');
    };

    window.openConfirmUpload = function (btn) {
        var ticketId = btn.getAttribute('data-ticket-id');
        var project = btn.getAttribute('data-project') || '';
        var form = document.getElementById('confirmUploadForm');
        if (!form) {
            alert('上传表单未找到，请 Ctrl+F5 强制刷新后重试');
            return;
        }
        var label = document.getElementById('confirmUploadProject');
        if (label) {
            label.textContent = '项目：' + project;
        }
        form.action = appendCsrfToAction(form, '/tickets/' + ticketId + '/confirmation');
        var fileInput = form.querySelector('input[type="file"]');
        if (fileInput) {
            fileInput.value = '';
        }
        openDialog('confirmUploadDialog');
    };

    window.openReportListModal = function (btn, kind) {
        var ticketId = btn.getAttribute('data-ticket-id');
        var project = btn.getAttribute('data-project') || '';
        var isPlan = kind === 'plan';
        var typeParam = isPlan ? 'PLAN_CONFIRM' : 'CONFIRM';
        var lockLabel = isPlan ? '方案确认报告' : '完成确认报告';
        var titleEl = document.getElementById('reportListModalTitle');
        var projectEl = document.getElementById('reportListProject');
        var hintEl = document.getElementById('reportListHint');
        var loadingEl = document.getElementById('reportListLoading');
        var emptyEl = document.getElementById('reportListEmpty');
        var listEl = document.getElementById('reportListItems');
        if (!titleEl || !loadingEl || !emptyEl || !listEl) {
            alert('报告列表弹窗未找到，请 Ctrl+F5 强制刷新后重试');
            return;
        }
        titleEl.textContent = lockLabel;
        if (projectEl) {
            projectEl.textContent = '项目：' + project;
        }
        if (hintEl) {
            hintEl.textContent = isPlan
                ? '可查看、确认存档或删除未存档文件。请上传双方确认后的方案文件。'
                : '可查看、确认存档或删除未存档文件。请上传完成后的确认文件。';
        }
        emptyEl.textContent = '暂无报告';
        loadingEl.classList.remove('d-none');
        emptyEl.classList.add('d-none');
        listEl.classList.add('d-none');
        listEl.innerHTML = '';
        openDialog('reportListDialog');

        fetch('/tickets/' + encodeURIComponent(ticketId) + '/report-attachments?type=' + encodeURIComponent(typeParam), {
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        }).then(function (resp) {
            if (!resp.ok) {
                throw new Error('load failed');
            }
            return resp.json();
        }).then(function (rows) {
            loadingEl.classList.add('d-none');
            if (!rows || !rows.length) {
                emptyEl.classList.remove('d-none');
                return;
            }
            var csrf = findPageCsrf();
            var confirmedTagClass = isPlan ? 'tag-primary' : 'tag-success';
            listEl.innerHTML = rows.map(function (r) {
                var meta = [(r.createBy || ''), (r.createTime || '')].filter(Boolean).join(' · ');
                var actions = '';
                if (r.confirmed) {
                    actions = '<span class="tag ' + confirmedTagClass + ' ms-1">已存档</span>';
                } else {
                    var confirmAction = '/tickets/attachments/' + encodeURIComponent(r.id) + '/confirm';
                    var deleteAction = '/tickets/attachments/' + encodeURIComponent(r.id) + '/delete';
                    var csrfField = csrf
                        ? '<input type="hidden" name="' + escapeHtml(csrf.name) + '" value="' + escapeHtml(csrf.value) + '">'
                        : '';
                    actions =
                        '<form action="' + confirmAction + '" method="post" class="d-inline ms-2"'
                        + ' onsubmit="return confirm(\'确认后将存档该' + lockLabel + '。确定？\')">'
                        + csrfField
                        + '<input type="hidden" name="ticketId" value="' + escapeHtml(ticketId) + '">'
                        + '<input type="hidden" name="returnTo" value="list">'
                        + '<button type="submit" class="btn btn-link btn-sm p-0">确认存档</button></form>'
                        + '<form action="' + deleteAction + '" method="post" class="d-inline ms-2"'
                        + ' onsubmit="return confirm(\'确定删除该' + lockLabel + '？可重新上传。\')">'
                        + csrfField
                        + '<input type="hidden" name="ticketId" value="' + escapeHtml(ticketId) + '">'
                        + '<input type="hidden" name="returnTo" value="list">'
                        + '<button type="submit" class="btn btn-link btn-sm text-danger p-0">删除</button></form>';
                }
                return '<li class="attachment-file-item">'
                    + '<a class="link-tech" href="/tickets/attachments/' + encodeURIComponent(r.id) + '">'
                    + escapeHtml(r.originalName) + '</a>'
                    + (meta ? '<span class="text-muted small ms-2">' + escapeHtml(meta) + '</span>' : '')
                    + actions
                    + '</li>';
            }).join('');
            listEl.classList.remove('d-none');
        }).catch(function () {
            loadingEl.classList.add('d-none');
            emptyEl.textContent = '加载失败，请刷新后重试';
            emptyEl.classList.remove('d-none');
        });
    };

    window.appendCsrfToAction = appendCsrfToAction;
    window.showBsModal = function (modalId) {
        var el = document.getElementById(modalId);
        if (!el) {
            alert('页面弹窗未加载，请 Ctrl+F5 强制刷新后重试');
            return;
        }
        if (window.bootstrap && bootstrap.Modal) {
            bootstrap.Modal.getOrCreateInstance(el).show();
            return;
        }
        openDialog(modalId);
    };

    document.addEventListener('DOMContentLoaded', function () {
        bindDialogCloseButtons(document);
    });
})();
