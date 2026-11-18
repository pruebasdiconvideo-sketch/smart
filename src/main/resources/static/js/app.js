/* Helpers para consumir la API desde las vistas (cookie JWT HttpOnly) */
async function api(url, opts={}) {
  opts.credentials = 'same-origin';
  opts.headers = opts.headers || {};
  if (opts.body && !(opts.body instanceof FormData)) opts.headers['Content-Type'] = 'application/json';
  const res = await fetch(url, opts);
  if (res.status === 401) { window.location.href = '/login?error=sesion'; return null; }
  if (res.status === 403) { alert('Su rol no tiene permiso para esta operación.'); return null; }
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('application/json')) {
    const data = await res.json();
    if (!res.ok) throw new Error(data.mensaje || data.error || 'Error');
    return data;
  }
  if (ct.includes('application/pdf')) return await res.blob();
  return res.ok ? null : (() => { throw new Error('HTTP ' + res.status); })();
}
function postJson(url, body){ return api(url, {method:'POST', body: JSON.stringify(body)}); }
function putJson(url, body){ return api(url, {method:'PUT', body: JSON.stringify(body)}); }
function patch(url){ return api(url, {method:'PATCH'}); }
function getJson(url){ return api(url, {method:'GET'}); }
function el(id){ return document.getElementById(id); }
function esc(s){ return (s==null?'':String(s)).replace(/[&<>"]/g, c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c])); }
function fmtMonto(n){ if(n==null) return ''; return 'S/ ' + Number(n).toLocaleString('es-PE',{minimumFractionDigits:2, maximumFractionDigits:2}); }
function fmtFecha(s){ if(!s) return ''; return new Date(s).toLocaleString('es-PE',{dateStyle:'medium', timeStyle:'short'}); }
function modalSelectorMarkup(id, placeholder) {
  return `<div class="input-group">
    <input type="hidden" id="${esc(id)}"/>
    <input type="text" class="form-control" id="${esc(id)}Display" placeholder="${esc(placeholder)}" aria-label="${esc(placeholder)}" readonly/>
    <button class="btn btn-outline-primary" type="button" id="${esc(id)}Select">Seleccionar</button>
  </div>`;
}
function initModalSelector(id, label, options) {
  const valueField = el(id);
  const displayField = el(id + 'Display');
  const selectButton = el(id + 'Select');
  const normalizedOptions = (options || []).map(option => ({
    value: String(option.value),
    label: String(option.label)
  }));
  const modal = document.createElement('div');
  modal.className = 'modal fade';
  modal.tabIndex = -1;
  modal.id = 'modal-selector-' + id;
  modal.setAttribute('role', 'dialog');
  modal.setAttribute('aria-modal', 'true');
  modal.setAttribute('aria-hidden', 'true');

  const dialog = document.createElement('div');
  dialog.className = 'modal-dialog modal-dialog-scrollable';
  const content = document.createElement('div');
  content.className = 'modal-content';
  const header = document.createElement('div');
  header.className = 'modal-header';
  const title = document.createElement('h5');
  title.className = 'modal-title';
  title.id = modal.id + '-title';
  title.textContent = 'Seleccionar ' + label;
  modal.setAttribute('aria-labelledby', title.id);
  const closeButton = document.createElement('button');
  closeButton.type = 'button';
  closeButton.className = 'btn-close';
  closeButton.setAttribute('data-bs-dismiss', 'modal');
  closeButton.setAttribute('aria-label', 'Cerrar');
  header.append(title, closeButton);

  const body = document.createElement('div');
  body.className = 'modal-body';
  const search = document.createElement('input');
  search.type = 'search';
  search.className = 'form-control mb-3';
  search.placeholder = 'Buscar...';
  search.setAttribute('aria-label', 'Buscar ' + label);
  const list = document.createElement('div');
  list.className = 'list-group modal-selector-options';
  body.append(search, list);

  const footer = document.createElement('div');
  footer.className = 'modal-footer';
  const cancelButton = document.createElement('button');
  cancelButton.type = 'button';
  cancelButton.className = 'btn btn-secondary';
  cancelButton.textContent = 'Cancelar';
  cancelButton.setAttribute('data-bs-dismiss', 'modal');
  footer.append(cancelButton);
  content.append(header, body, footer);
  dialog.append(content);
  modal.append(dialog);
  document.body.append(modal);

  const modalInstance = new bootstrap.Modal(modal);
  function renderOptions() {
    const query = search.value.trim().toLocaleLowerCase();
    const matches = normalizedOptions.filter(option => option.label.toLocaleLowerCase().includes(query));
    list.replaceChildren();
    if (matches.length === 0) {
      const empty = document.createElement('p');
      empty.className = 'text-muted mb-0';
      empty.textContent = normalizedOptions.length ? 'No se encontraron resultados.' : 'No hay opciones disponibles.';
      list.append(empty);
      return;
    }
    matches.forEach(option => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'list-group-item list-group-item-action modal-selector-option';
      button.textContent = option.label;
      button.addEventListener('click', () => {
        valueField.value = option.value;
        displayField.value = option.label;
        valueField.dispatchEvent(new Event('change', {bubbles: true}));
        modalInstance.hide();
      });
      list.append(button);
    });
  }
  search.addEventListener('input', renderOptions);
  modal.addEventListener('shown.bs.modal', () => search.focus());
  selectButton.disabled = normalizedOptions.length === 0;
  selectButton.addEventListener('click', () => {
    search.value = '';
    renderOptions();
    modalInstance.show();
  });
  if (normalizedOptions.length > 0) {
    valueField.value = normalizedOptions[0].value;
    displayField.value = normalizedOptions[0].label;
  } else {
    displayField.placeholder = 'No hay opciones disponibles';
  }
}
// Logout por link (no es formulario)
document.addEventListener('click', e => {
  const a = e.target.closest('a[data-method="post"]');
  if (a) { e.preventDefault(); api(a.href, {method:'POST'}).then(()=> window.location.href='/login?logout'); }
});
