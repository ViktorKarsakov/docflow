// ============================================================
// DOCFLOW — API клиент
// Централизованный fetch-обёртка с обработкой ошибок и авторизации
// ============================================================

const API = {

  // Базовый URL для всех API запросов
  BASE: '/api',

  // --- Внутренний метод выполнения запроса ---
  async request(method, path, body = null) {
    const options = {
      method,
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
    };

    if (body !== null) {
      options.body = JSON.stringify(body);
    }

    let response;
    try {
      response = await fetch(this.BASE + path, options);
    } catch (e) {
      throw new Error('Нет соединения с сервером');
    }

    // 401 — не авторизован, редирект на логин
    if (response.status === 401) {
      window.location.href = '/pages/login.html';
      return;
    }

    // 403 — доступ запрещён
    if (response.status === 403) {
      throw new Error('Доступ запрещён');
    }

    // 204 — нет тела ответа
    if (response.status === 204) {
      return null;
    }

    let data;
    try {
      data = await response.json();
    } catch {
      if (!response.ok) throw new Error('Ошибка сервера');
      return null;
    }

    if (!response.ok) {
      // Сервер вернул ошибку с описанием
      const msg = data?.error || data?.message || 'Произошла ошибка';
      throw new Error(msg);
    }

    return data;
  },

  get:    (path)        => API.request('GET',    path),
  post:   (path, body)  => API.request('POST',   path, body),
  put:    (path, body)  => API.request('PUT',    path, body),
  delete: (path)        => API.request('DELETE', path),

  // ============================================================
  // АУТЕНТИФИКАЦИЯ
  // ============================================================
  auth: {
    me: () => API.get('/auth/me'),
  },

  // ============================================================
  // ДОКУМЕНТЫ
  // ============================================================
  documents: {
    getMyDocuments:   ()          => API.get('/documents/my'),
    getInbox:         ()          => API.get('/documents/inbox'),
    getAll:           ()          => API.get('/documents/all'),
    getById:          (id)        => API.get(`/documents/${id}`),
    create:           (data)      => API.post('/documents', data),
    submit:           (id)        => API.post(`/documents/${id}/submit`),
    processDecision:  (id, data)  => API.post(`/documents/${id}/decision`, data),
    withdraw:         (id)        => API.post(`/documents/${id}/withdraw`),
    search:           (params)    => {
      const q = new URLSearchParams(
        Object.fromEntries(Object.entries(params).filter(([,v]) => v !== null && v !== '' && v !== undefined))
      );
      return API.get(`/documents/search?${q}`);
    },
  },

  // ============================================================
  // ПОРУЧЕНИЯ
  // ============================================================
  tasks: {
    getMyTasks:   ()             => API.get('/tasks/my'),
    getIssuedByMe:()             => API.get('/tasks/issued'),
    create:       (data)         => API.post('/tasks', data),
    start:        (id)           => API.post(`/tasks/${id}/start`),
    complete:     (id, report)   => API.post(`/tasks/${id}/complete`, { report }),
    cancel:       (id)           => API.post(`/tasks/${id}/cancel`),
  },

  // ============================================================
  // УВЕДОМЛЕНИЯ
  // ============================================================
  notifications: {
    getAll:       ()   => API.get('/notifications'),
    getUnreadCount:()  => API.get('/notifications/unread-count'),
    markRead:     (id) => API.post(`/notifications/${id}/read`),
    markAllRead:  ()   => API.post('/notifications/read-all'),
  },

  // ============================================================
  // ПОЛЬЗОВАТЕЛИ
  // ============================================================
  users: {
    getAll:   ()         => API.get('/users'),
    getById:  (id)       => API.get(`/users/${id}`),
    create:   (data)     => API.post('/users', data),
    update:   (id, data) => API.put(`/users/${id}`, data),
    deactivate:(id)      => API.delete(`/users/${id}`),
    search:   (query)    => API.get(`/users/search?query=${encodeURIComponent(query)}`),
  },

  // ============================================================
  // ОТДЕЛЫ
  // ============================================================
  departments: {
    getAll: () => API.get('/departments'),
  },

  // ============================================================
  // ТИПЫ ДОКУМЕНТОВ
  // ============================================================
  documentTypes: {
    getAll: () => API.get('/document-types'),
  },

  // ============================================================
  // РОЛИ
  // ============================================================
  roles: {
    getAll: () => API.get('/roles'),
  },

  // ============================================================
  // ШАБЛОНЫ МАРШРУТОВ
  // ============================================================
  routeTemplates: {
    getAll:   ()         => API.get('/route-templates'),
    getById:  (id)       => API.get(`/route-templates/${id}`),
    create:   (data)     => API.post('/route-templates', data),
    update:   (id, data) => API.put(`/route-templates/${id}`, data),
    delete:   (id)       => API.delete(`/route-templates/${id}`),
  },
};
