/* =====================================================================
   Smart Power On — Helpers comunes para todas las vistas
   - api/postJson/putJson/patch/getJson: cliente REST con cookie JWT HttpOnly
   - el/esc/fmtMonto/fmtFecha: utilidades de UI
   - modalSelectorMarkup/initModalSelector: modal de búsqueda y selección
     (con resaltado, contador de resultados y navegación por teclado)
   - spToast: notificaciones no intrusivas
   ===================================================================== */

/* ---------- Cliente REST ---------- */
async function api(url, opts = {}) {
  opts.credentials = 'same-origin';
  opts.headers = opts.headers || {};
  if (opts.body && !(opts.body instanceof FormData)) opts.headers['Content-Type'] = 'application/json';
  const res = await fetch(url, opts);
  if (res.status === 401) { window.location.href = '/login?error=sesion'; return null; }
  if (res.status === 403) { spToast('Su rol no tiene permiso para esta operación.', 'danger'); return null; }
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('application/json')) {
    const data = await res.json();
    if (!res.ok) throw new Error(data.mensaje || data.error || 'Error');
    return data;
  }
  if (ct.includes('application/pdf')) return await res.blob();
  return res.ok ? null : (() => { throw new Error('HTTP ' + res.status); })();
}
function postJson(url, body) { return api(url, { method: 'POST', body: JSON.stringify(body) }); }
function putJson(url, body)  { return api(url, { method: 'PUT',  body: JSON.stringify(body) }); }
function patch(url)          { return api(url, { method: 'PATCH' }); }
function getJson(url)        { return api(url, { method: 'GET' }); }

/* ---------- Utilidades de UI ---------- */
function el(id) { return document.getElementById(id); }
function esc(s) {
  return (s == null ? '' : String(s)).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
}
function fmtMonto(n) {
  if (n == null) return '';
  return 'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
function fmtFecha(s) {
  if (!s) return '';
  return new Date(s).toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' });
}

/* ---------- Toast ---------- */
function spToast(msg, type) {
  let host = document.querySelector('.toast-container');
  if (!host) {
    host = document.createElement('div');
    host.className = 'toast-container';
    document.body.appendChild(host);
  }
  const icons = { success: 'check-circle-fill', danger: 'x-circle-fill', warning: 'exclamation-triangle-fill', info: 'info-circle-fill' };
  const titles = { success: 'Correcto', danger: 'Error', warning: 'Atención', info: 'Información' };
  const t = document.createElement('div');
  t.className = 'sp-toast ' + (type || 'info');
  const icon = icons[type || 'info'] || icons.info;
  const title = titles[type || 'info'] || titles.info;
  t.innerHTML = `<p class="toast-title"><i class="bi bi-${icon}"></i> ${title}</p><p class="toast-msg"></p>`;
  t.querySelector('.toast-msg').textContent = String(msg);
  host.appendChild(t);
  setTimeout(() => {
    t.style.opacity = '0';
    t.style.transition = 'opacity .25s ease';
    setTimeout(() => t.remove(), 250);
  }, 4000);
}

/* ---------- Modal selector (búsqueda + selección) ----------
   Uso:
     modalSelectorMarkup('material', 'Seleccione un material')
     initModalSelector('material', 'material', listaDeOpciones)
   listaDeOpciones = [{ value, label, meta? }]
*/
function modalSelectorMarkup(id, placeholder) {
  return `<div class="input-group">
    <input type="hidden" id="${esc(id)}"/>
    <input type="text" class="form-control" id="${esc(id)}Display" placeholder="${esc(placeholder)}" aria-label="${esc(placeholder)}" readonly/>
    <button class="btn btn-outline-primary" type="button" id="${esc(id)}Select">
      <i class="bi bi-search"></i> Seleccionar
    </button>
  </div>`;
}

function initModalSelector(id, label, options) {
  const valueField = el(id);
  const displayField = el(id + 'Display');
  const selectButton = el(id + 'Select');
  const normalizedOptions = (options || []).map(option => ({
    value: String(option.value),
    label: String(option.label),
    meta: option.meta ? String(option.meta) : null
  }));

  // Construye el modal una sola vez
  const modal = document.createElement('div');
  modal.className = 'modal fade';
  modal.tabIndex = -1;
  modal.id = 'modal-selector-' + id;
  modal.setAttribute('role', 'dialog');
  modal.setAttribute('aria-modal', 'true');
  modal.setAttribute('aria-hidden', 'true');

  modal.innerHTML = `
    <div class="modal-dialog modal-dialog-scrollable">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="${modal.id}-title">
            <i class="bi bi-search"></i> Seleccionar ${esc(label)}
          </h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
        </div>
        <div class="modal-body">
          <div class="modal-selector-search">
            <i class="bi bi-search"></i>
            <input type="search" class="form-control" placeholder="Escriba para filtrar..." aria-label="Buscar ${esc(label)}"/>
          </div>
          <div class="modal-selector-meta">
            <span class="result-count"></span>
            <span class="hint text-muted">↑↓ navegar · Enter seleccionar · Esc cerrar</span>
          </div>
          <div class="modal-selector-options"></div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
            <i class="bi bi-x-lg"></i> Cancelar
          </button>
        </div>
      </div>
    </div>`;

  document.body.append(modal);
  const modalInstance = new bootstrap.Modal(modal);

  const search = modal.querySelector('input[type="search"]');
  const list = modal.querySelector('.modal-selector-options');
  const countEl = modal.querySelector('.result-count');
  let kbIndex = -1;
  let currentMatches = [];

  function escapeRegExp(s) { return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); }
  function highlight(text, query) {
    if (!query) return esc(text);
    const re = new RegExp('(' + escapeRegExp(query) + ')', 'ig');
    return esc(text).replace(re, '<mark>$1</mark>');
  }

  function renderOptions() {
    const query = search.value.trim();
    const queryLower = query.toLowerCase();
    currentMatches = normalizedOptions.filter(option => option.label.toLowerCase().includes(queryLower));
    kbIndex = -1;
    list.replaceChildren();

    if (currentMatches.length === 0) {
      const empty = document.createElement('div');
      empty.className = 'empty-state';
      empty.innerHTML = `<i class="bi bi-inbox"></i><p class="mb-0">${normalizedOptions.length ? 'No se encontraron resultados.' : 'No hay opciones disponibles.'}</p>`;
      list.append(empty);
      countEl.textContent = normalizedOptions.length ? '0 de ' + normalizedOptions.length : 'Sin opciones';
      return;
    }

    countEl.textContent = currentMatches.length + ' de ' + normalizedOptions.length + (currentMatches.length === 1 ? ' resultado' : ' resultados');

    currentMatches.forEach((option, idx) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'modal-selector-option';
      button.dataset.index = idx;
      button.innerHTML = highlight(option.label, query) +
        (option.meta ? `<span class="opt-meta">${esc(option.meta)}</span>` : '');
      button.addEventListener('click', () => selectOption(idx));
      button.addEventListener('mouseenter', () => setKbIndex(idx));
      list.append(button);
    });
  }

  function setKbIndex(idx) {
    const buttons = list.querySelectorAll('.modal-selector-option');
    buttons.forEach(b => b.classList.remove('kb-active'));
    if (idx >= 0 && idx < buttons.length) {
      buttons[idx].classList.add('kb-active');
      buttons[idx].scrollIntoView({ block: 'nearest' });
    }
    kbIndex = idx;
  }

  function selectOption(idx) {
    const option = currentMatches[idx];
    if (!option) return;
    valueField.value = option.value;
    displayField.value = option.label;
    valueField.dispatchEvent(new Event('change', { bubbles: true }));
    modalInstance.hide();
  }

  // Búsqueda
  search.addEventListener('input', renderOptions);

  // Navegación por teclado
  search.addEventListener('keydown', e => {
    if (currentMatches.length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setKbIndex(Math.min(kbIndex + 1, currentMatches.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setKbIndex(Math.max(kbIndex - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      selectOption(kbIndex >= 0 ? kbIndex : 0);
    } else if (e.key === 'Escape') {
      e.preventDefault();
      modalInstance.hide();
    }
  });

  // Focus al abrir
  modal.addEventListener('shown.bs.modal', () => {
    search.value = '';
    renderOptions();
    setTimeout(() => search.focus(), 50);
  });

  selectButton.disabled = normalizedOptions.length === 0;
  selectButton.addEventListener('click', () => modalInstance.show());

  // Estado inicial
  if (normalizedOptions.length > 0) {
    valueField.value = normalizedOptions[0].value;
    displayField.value = normalizedOptions[0].label;
  } else {
    displayField.placeholder = 'No hay opciones disponibles';
  }
}

/* ---------- Logout por link (data-method="post") ---------- */
document.addEventListener('click', e => {
  const a = e.target.closest('a[data-method="post"]');
  if (a) {
    e.preventDefault();
    api(a.href, { method: 'POST' }).then(() => { window.location.href = '/login?logout'; });
  }
});

/* ---------- Cerrar sidebar al navegar en móvil ---------- */
document.addEventListener('click', e => {
  const sidebar = document.getElementById('sidebar');
  const link = e.target.closest('.sidebar .nav-link');
  if (sidebar && link && window.matchMedia('(max-width: 992px)').matches) {
    sidebar.classList.remove('show');
  }
});
