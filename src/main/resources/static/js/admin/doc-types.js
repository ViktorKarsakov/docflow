// ============================================================
// DOCFLOW — Управление типами документов
// ============================================================

const DocTypesAdmin = {

  _editingId: null,
  _routeTemplates: [], // Кэш шаблонов маршрутов для отображения в таблице

  async load() {
    const container = document.getElementById('doctypes-table-body');
    if (!container) return;

    container.innerHTML = '<tr><td colspan="6"><div class="loading-overlay"><div class="spinner"></div></div></td></tr>';

    try {
      // Загружаем типы и маршруты параллельно — чтобы показать в таблице есть ли маршрут
      const [types, routes] = await Promise.all([
        API.get('/document-types/all'),
        API.routeTemplates.getAll().catch(() => []),
      ]);
      this._routeTemplates = routes;

      container.innerHTML = types.map(t => {
        // Проверяем есть ли маршрут для этого типа
        const hasRoute = routes.some(r => r.documentTypeId === t.id);
        const routeBadge = hasRoute
          ? '<span class="badge badge-approved"><i class="bi bi-check2"></i> Настроен</span>'
          : '<span class="badge badge-warning" style="background:var(--warning-bg);color:var(--warning);border:1px solid var(--warning-border)"><i class="bi bi-exclamation"></i> Не настроен</span>';

        return `
          <tr>
            <td class="fw-500">${t.displayName}</td>
            <td><span class="badge badge-draft">${t.code}</span></td>
            <td><span class="doc-reg-number">${t.prefix}</span></td>
            <td>${routeBadge}</td>
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
        `;
      }).join('');
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  openCreate() {
    this._editingId = null;
    document.getElementById('doctype-modal-title').textContent = 'Новый тип документа';
    document.getElementById('doctype-form').reset();
    // При создании кнопка маршрута скрыта — сначала нужно сохранить тип
    document.getElementById('route-btn-wrap').style.display = 'none';
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
      // При редактировании показываем кнопку настройки маршрута
      document.getElementById('route-btn-wrap').style.display = '';
      showModal('doctype-modal');
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  // Открыть страницу редактора маршрута для текущего типа документа
  openRouteEditor() {
    if (!this._editingId) return;
    // Сначала сохраняем текущие изменения типа, потом переходим
    hideModal('doctype-modal');
    window.location.href = `/pages/admin/route-editor.html?typeId=${this._editingId}`;
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
        const created = await API.post('/document-types', data);
        showToast('Тип создан. Теперь откройте редактирование и настройте маршрут.', 'success');
        this._editingId = created.id;
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
