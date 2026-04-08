// ============================================================
// DOCFLOW — Авторизация и текущий пользователь
// ============================================================

const Auth = {

  // Текущий пользователь (загружается один раз)
  _user: null,

  // Получить текущего пользователя
  async getUser() {
    if (this._user) return this._user;
    try {
      this._user = await API.auth.me();
      return this._user;
    } catch {
      window.location.href = '/pages/login.html';
      return null;
    }
  },

  // Проверить наличие роли
  hasRole(user, role) {
    return user?.roles?.includes(role) ?? false;
  },

  // Проверить наличие одной из ролей
  hasAnyRole(user, ...roles) {
    return roles.some(r => this.hasRole(user, r));
  },

  // Выйти из системы
  async logout() {
    try {
      await fetch('/logout', {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          'X-Requested-With': 'XMLHttpRequest',
        }
      });
    } finally {
      this._user = null;
      window.location.href = '/pages/login.html';
    }
  },

  // Получить инициалы пользователя для аватара
  getInitials(fullName) {
    if (!fullName) return '?';
    const parts = fullName.trim().split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return fullName.substring(0, 2).toUpperCase();
  },

  // Отображаемое название роли
  getRoleDisplayName(role) {
    const names = {
      'ROLE_ADMIN':      'Администратор',
      'ROLE_CHIEF':      'Главный врач',
      'ROLE_ECONOMIST':  'Экономист',
      'ROLE_HR':         'Отдел кадров',
      'ROLE_ACCOUNTING': 'Бухгалтерия',
      'ROLE_PURCHASE':   'Отдел закупок',
      'ROLE_IT':         'ИАО',
      'ROLE_LAWYER':     'Юридический отдел',
      'ROLE_EMPLOYEE':   'Сотрудник',
    };
    return names[role] || role;
  }
};
