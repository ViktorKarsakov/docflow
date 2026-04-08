// ============================================================
// DOCFLOW — Логика страниц документов
// ============================================================

const Documents = {

  // ============================================================
  // МОИ ДОКУМЕНТЫ
  // ============================================================
  async loadMyDocuments(containerId, filterStatus = '') {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = '<div class="loading-overlay"><div class="spinner"></div></div>';

    try {
      let docs = await API.documents.getMyDocuments();

      // Фильтрация по статусу
      if (filterStatus) {
        docs = docs.filter(d => d.status === filterStatus);
      }

      if (!docs.length) {
        container.innerHTML = `<div class="empty-state">
          <i class="bi bi-file-earmark-x"></i>
          <p>Документов не найдено</p>
        </div>`;
        return;
      }

      container.innerHTML = `
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Документ</th>
                <th>Тип</th>
                <th>Статус</th>
                <th>Создан</th>
                <th>Дедлайн</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              ${docs.map(d => this.renderDocRow(d)).join('')}
            </tbody>
          </table>
        </div>
      `;
    } catch (e) {
      container.innerHTML = `<div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i>${e.message}</div>`;
    }
  },

  // ============================================================
  // ВХОДЯЩИЕ
  // ============================================================
  async loadInbox(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = '<div class="loading-overlay"><div class="spinner"></div></div>';

    try {
      const docs = await API.documents.getInbox();

      if (!docs.length) {
        container.innerHTML = `<div class="empty-state">
          <i class="bi bi-inbox"></i>
          <p>Нет документов, ожидающих вашего решения</p>
        </div>`;
        return;
      }

      container.innerHTML = `
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Документ</th>
                <th>Тип</th>
                <th>Автор</th>
                <th>Отправлен</th>
                <th>Дедлайн</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              ${docs.map(d => `
                <tr class="${d.overdue ? 'overdue-row' : ''}">
                  <td>
                    <div class="doc-row-title">
                      <a href="/pages/document-view.html?id=${d.id}">${d.title}</a>
                    </div>
                    <div class="doc-row-meta">
                      ${d.registrationNumber ? `<span class="doc-reg-number">${d.registrationNumber}</span>` : ''}
                    </div>
                  </td>
                  <td class="text-sec">${d.documentTypeName}</td>
                  <td class="text-sec">${d.authorFullName}</td>
                  <td class="text-muted">${formatDateTime(d.submittedAt)}</td>
                  <td>${d.deadline ? `<span class="${d.overdue ? 'text-danger' : 'text-sec'}">${formatDate(d.deadline)}</span>` : '—'}</td>
                  <td class="actions">
                    <a href="/pages/document-view.html?id=${d.id}" class="btn btn-primary btn-sm">
                      <i class="bi bi-eye"></i> Рассмотреть
                    </a>
                  </td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      `;
    } catch (e) {
      container.innerHTML = `<div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i>${e.message}</div>`;
    }
  },

  // ============================================================
  // ВСЕ ДОКУМЕНТЫ (для главврача и админа)
  // ============================================================
  async loadAll(containerId, searchParams = {}) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = '<div class="loading-overlay"><div class="spinner"></div></div>';

    try {
      const hasSearch = Object.values(searchParams).some(v => v !== '' && v !== null && v !== undefined);
      const docs = hasSearch
        ? await API.documents.search(searchParams)
        : await API.documents.getAll();

      if (!docs.length) {
        container.innerHTML = `<div class="empty-state"><i class="bi bi-search"></i><p>Документов не найдено</p></div>`;
        return;
      }

      container.innerHTML = `
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Документ</th>
                <th>Тип</th>
                <th>Статус</th>
                <th>Автор</th>
                <th>Создан</th>
                <th>Дедлайн</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              ${docs.map(d => this.renderDocRow(d, true)).join('')}
            </tbody>
          </table>
        </div>
      `;
    } catch (e) {
      container.innerHTML = `<div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i>${e.message}</div>`;
    }
  },

  // --- Строка таблицы документа ---
  renderDocRow(d, showAuthor = false) {
    return `
      <tr class="${d.overdue ? 'overdue-row' : ''}">
        <td>
          <div class="doc-row-title">
            <a href="/pages/document-view.html?id=${d.id}">${d.title}</a>
          </div>
          <div class="doc-row-meta">
            ${d.registrationNumber ? `<span class="doc-reg-number">${d.registrationNumber}</span>` : ''}
            ${d.overdue ? ' <i class="bi bi-exclamation-circle text-danger" title="Просрочен"></i>' : ''}
          </div>
        </td>
        <td class="text-sec">${d.documentTypeName}</td>
        <td>${docStatusBadge(d.status)}</td>
        ${showAuthor ? `<td class="text-sec">${d.authorFullName}</td>` : ''}
        <td class="text-muted">${formatDate(d.createdAt)}</td>
        <td>${d.deadline ? `<span class="${d.overdue ? 'text-danger fw-600' : 'text-sec'}">${formatDate(d.deadline)}</span>` : '<span class="text-muted">—</span>'}</td>
        <td class="actions">
          <a href="/pages/document-view.html?id=${d.id}" class="btn btn-ghost btn-sm btn-icon" title="Открыть">
            <i class="bi bi-eye"></i>
          </a>
        </td>
      </tr>
    `;
  },

  // ============================================================
  // ПРОСМОТР ДОКУМЕНТА
  // ============================================================
  async loadDocumentView(docId) {
    try {
      const doc = await API.documents.getById(docId);
      this.renderDocumentView(doc);
      return doc;
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  renderDocumentView(doc) {
    // Заголовок
    const title = document.getElementById('doc-title');
    if (title) title.textContent = doc.title;

    // Тип
    const type = document.getElementById('doc-type');
    if (type) type.textContent = doc.documentTypeName;

    // Контент
    const content = document.getElementById('doc-content');
    if (content) content.textContent = doc.content;

    // Инфо-строки
    const infoMap = {
      'info-status':    () => docStatusBadge(doc.status),
      'info-reg':       () => doc.registrationNumber || '—',
      'info-author':    () => doc.authorFullName,
      'info-created':   () => formatDate(doc.createdAt),
      'info-submitted': () => doc.submittedAt ? formatDateTime(doc.submittedAt) : '—',
      'info-deadline':  () => doc.deadline ? `<span class="${doc.overdue ? 'text-danger fw-600' : ''}">${formatDate(doc.deadline)}${doc.overdue ? ' <i class="bi bi-exclamation-triangle"></i>' : ''}</span>` : '—',
      'info-addressee': () => doc.addressee || '—',
      'info-count':     () => doc.submissionCount > 0 ? `${doc.submissionCount} раз(а)` : '—',
    };

    Object.entries(infoMap).forEach(([id, fn]) => {
      const el = document.getElementById(id);
      if (el) el.innerHTML = fn();
    });

    // Причина отклонения
    const rejBlock = document.getElementById('rejection-block');
    if (rejBlock) {
      if (doc.rejectionReason && doc.status === 'REJECTED') {
        rejBlock.classList.remove('hidden');
        const rejText = document.getElementById('rejection-reason');
        if (rejText) rejText.textContent = doc.rejectionReason;
      } else {
        rejBlock.classList.add('hidden');
      }
    }

    // История согласования
    const stepsContainer = document.getElementById('approval-steps');
    if (stepsContainer && doc.approvalSteps) {
      this.renderApprovalSteps(stepsContainer, doc.approvalSteps);
    }

    // Кнопки действий
    this.renderDocActions(doc);
  },

  renderApprovalSteps(container, steps) {
    if (!steps.length) {
      container.innerHTML = '<p class="text-muted" style="font-size:0.85rem">Шагов согласования нет</p>';
      return;
    }

    const stepTypeIcon = { APPROVAL: 'bi-check2', SIGNATURE: 'bi-pen', EXECUTION: 'bi-gear', RESOLUTION: 'bi-chat-left-dots' };
    const statusClass  = { PENDING: 'pending', ACTIVE: 'active', APPROVED: 'approved', REJECTED: 'rejected', SKIPPED: 'skipped' };

    container.innerHTML = `<div class="approval-timeline">
      ${steps.map(s => `
        <div class="approval-step">
          <div class="step-dot ${statusClass[s.status] || 'pending'}">
            <i class="bi ${stepTypeIcon[s.stepType] || 'bi-check2'}"></i>
          </div>
          <div class="step-content">
            <div class="step-name">${s.stepName}</div>
            <div class="step-meta">
              ${s.assignedRoleDisplayName}
              ${s.assigneeDepartmentName ? ` · ${s.assigneeDepartmentName}` : ''}
              · ${s.statusDisplayName}
              ${s.processedByFullName ? ` · ${s.processedByFullName}` : ''}
              ${s.processedAt ? ` · ${formatDateTime(s.processedAt)}` : ''}
            </div>
            ${s.comment ? `<div class="step-comment">${s.comment}</div>` : ''}
          </div>
        </div>
      `).join('')}
    </div>`;
  },

  renderDocActions(doc) {
    const submitBtn = document.getElementById('btn-submit');
    const withdrawBtn = document.getElementById('btn-withdraw');
    const decisionCard = document.getElementById('decision-card');

    // Кнопка отправить на согласование
    if (submitBtn) {
      submitBtn.style.display = (doc.status === 'DRAFT' || doc.status === 'REJECTED') ? '' : 'none';
    }

    // Кнопка отозвать
    if (withdrawBtn) {
      withdrawBtn.style.display = doc.status === 'ON_APPROVAL' ? '' : 'none';
    }

    // Блок принятия решения — определяется контроллером через /inbox
    // Если документ на согласовании — показываем (сервер проверит права)
    if (decisionCard) {
      decisionCard.style.display = doc.status === 'ON_APPROVAL' ? '' : 'none';
    }
  },

  // ============================================================
  // СОЗДАНИЕ ДОКУМЕНТА
  // ============================================================
  async createDraft(formData) {
    try {
      const doc = await API.documents.create(formData);
      showToast('Черновик сохранён', 'success');
      return doc;
    } catch (e) {
      showToast(e.message, 'error');
      return null;
    }
  },

  async submitDocument(docId) {
    try {
      await API.documents.submit(docId);
      showToast('Документ отправлен на согласование', 'success');
      return true;
    } catch (e) {
      showToast(e.message, 'error');
      return false;
    }
  },

  async processDecision(docId, approved, comment) {
    if (!approved && !comment?.trim()) {
      showToast('При отклонении необходимо указать причину', 'error');
      return false;
    }
    try {
      await API.documents.processDecision(docId, { approved, comment });
      showToast(approved ? 'Документ согласован' : 'Документ отклонён', approved ? 'success' : 'info');
      return true;
    } catch (e) {
      showToast(e.message, 'error');
      return false;
    }
  },

  async withdraw(docId) {
    try {
      await API.documents.withdraw(docId);
      showToast('Документ отозван', 'info');
      return true;
    } catch (e) {
      showToast(e.message, 'error');
      return false;
    }
  },
};
