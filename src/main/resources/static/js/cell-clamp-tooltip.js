(function () {
    function initCellClampTooltips() {
        if (typeof bootstrap === 'undefined') {
            return;
        }
        document.querySelectorAll('.data-table .cell-clamp').forEach(function (cell) {
            if (cell.dataset.clampInit) {
                return;
            }
            cell.dataset.clampInit = '1';
            if (cell.scrollWidth <= cell.clientWidth) {
                return;
            }
            const text = cell.textContent.trim();
            if (!text || text === '—') {
                return;
            }
            cell.setAttribute('data-bs-toggle', 'tooltip');
            cell.setAttribute('data-bs-placement', 'top');
            cell.setAttribute('data-bs-title', text);
            cell.setAttribute('data-bs-custom-class', 'cell-clamp-tooltip');
            cell.classList.add('cell-clamp-overflow');
            new bootstrap.Tooltip(cell, { trigger: 'hover', boundary: 'viewport' });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initCellClampTooltips);
    } else {
        initCellClampTooltips();
    }
})();
