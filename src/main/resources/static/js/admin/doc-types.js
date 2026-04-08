// ============================================================
// DOCFLOW — Управление типами документов
// ============================================================

const DocTypesAdmin = {

  _editingId: null,

  async load() {
    const container = document.getElementById('doctypes-table-body');
    if (!container) return;

    container.innerHTML = '<tr><td colspan="5"><div class="loading-overlay"><div class="spinner"></div></div></td></tr>';

    try {
      const types = await API.get('/document-types/all');
      container.innerHTML = types.map(t => `
        <tr>
          <td class="fw-500">${t.displayName}</td>
          <td><span class="badge badge-draft">${t.code}</span></td>
          <td><span class="doc-reg-number">${t.prefix}</span></td>
          <td>${t.active ? '<span class="badge badge-approved">Активен</span>' : '<span class="badge badge-withdrawn">Неактивен</span>'}</td>
          <td class="actions">
            <button class="btn btn-ghost btn-sm btn-icon" onclick="DocTypesAdmin.openEdit(${t.id})" title="Редактировать">
              <i class="bi bi-pencil"></i>
            </button>
            <button class="btn btn-ghost btn-sm btn-icon" onclick="DocTypesAdmin.toggleActive(${t.id}, ${!t.active})" title="${t.active ? 'Деактивировать' : 'Активировать'}">
              <i class="bi ${t.active ? 'bi-eye-slash' : 'bi-eye'}"></i>
            </button>
          </td>
        </tr>
      `).join('');
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  openCreate() {
    this._editingId = null;
    document.getElementById('doctype-modal-title').textContent = 'Новый тип документа';
    document.getElementById('doctype-form').reset();
    showModal('doctype-modal');
  },

  async openEdit(id) {
    try {
      const type = await API.get(`/document-types/${id}`);
      this._editingId = id;
      document.getElementById('doctype-modal-title').textContent = 'Редактировать тип';
      document.getElementById('dt-displayName').value = type.displayName;
      document.getElementById('dt-code').value = type.code;
      document.getElementById('dt-prefix').value = type.prefix;
      document.getElementById('dt-description').value = type.description || '';
      document.getElementById('dt-sortOrder').value = type.sortOrder;
      showModal('doctype-modal');
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  async save() {
    const data = {
      displayName:  document.getElementById('dt-displayName').value,
      code:         document.getElementById('dt-code').value,
      prefix:       document.getElementById('dt-prefix').value,
      description:  document.getElementById('dt-description').value || null,
      sortOrder:    Number(document.getElementById('dt-sortOrder').value) || 0,
      active:       true,
    };

    try {
      if (this._editingId) {
        await API.put(`/document-types/${this._editingId}`, data);
        showToast('Тип обновлён', 'success');
      } else {
        await API.post('/document-types', data);
        showToast('Тип создан', 'success');
      }
      hideModal('doctype-modal');
      await this.load();
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  async toggleActive(id, active) {
    try {
      await API.put(`/document-types/${id}/active`, { active });
      await this.load();
    } catch (e) {
      showToast(e.message, 'error');
    }
  },
};
