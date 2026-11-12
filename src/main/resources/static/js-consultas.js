/**
 * Hospital SIH | Consultas — integração exclusiva da tela de consultas.
 * O arquivo original js-app.js permanece intacto.
 * Usa exclusivamente os contratos existentes no AtendimentoController e DTOs Spring Boot.
 *
 * IMPORTANTE: para evitar CORS, abra http://localhost:8080/consultas.html
 * e sirva os arquivos pelo mesmo Spring Boot (src/main/resources/static).
 */
(function () {
  'use strict';

  // Mesma origem da aplicação Spring Boot. Não mudar para localhost:8080 se a página
  // já estiver sendo servida pelo Spring: isso quebraria deploys e HTTPS.
  const API = { consultas: '/consultas', pacientes: '/pacientes', profissionais: '/profissionais' };
  const MODALIDADES = {
    ROTINA: 'Consulta de rotina', ESPECIALIZADA: 'Consulta especializada',
    EMERGENCIA: 'Atendimento de emergência', CIRURGICO: 'Procedimento cirúrgico'
  };
  const SITUACOES = {
    AGENDADA: 'Agendada', CONFIRMADA: 'Confirmada', EM_ATENDIMENTO: 'Em atendimento',
    CONCLUIDA: 'Concluída', CANCELADA: 'Cancelada', NAO_COMPARECEU: 'Não compareceu'
  };
  const state = { pacientes: [], profissionais: [], consultas: [], selecionada: null, busy: false, carregouAgenda: false };
  const $ = (selector) => document.querySelector(selector);
  const byId = (id) => document.getElementById(id);
  let toastTimer;

  function notify(message, isError = false) {
    const el = $('.toast');
    if (!el) return;
    clearTimeout(toastTimer);
    el.textContent = message;
    el.classList.toggle('is-error', isError);
    el.classList.add('show');
    toastTimer = setTimeout(() => el.classList.remove('show'), 4700);
  }

  function apiIndicator(status, text) {
    const indicator = byId('api-state');
    indicator.classList.toggle('is-connected', status === 'connected');
    indicator.classList.toggle('is-error', status === 'error');
    indicator.lastChild.textContent = text;
  }

  async function request(path, options) {
    const response = await fetch(path, {
      headers: { 'Accept': 'application/json', ...(options && options.body ? { 'Content-Type': 'application/json' } : {}) },
      ...options
    });
    const raw = await response.text();
    let result = null;
    if (raw) {
      try { result = JSON.parse(raw); }
      catch (_) {
        if (!response.ok) throw new Error(`Erro HTTP ${response.status}. O servidor não retornou JSON válido.`);
        throw new Error('A API retornou um conteúdo inesperado. Confirme o endereço da aplicação.');
      }
    }
    if (!response.ok) {
      let reason = result && (result.erro || result.message || result.error);
      if (!reason && result && result.campos) reason = Object.entries(result.campos).map(([key, val]) => `${key}: ${val}`).join(' · ');
      throw new Error(reason || `Erro HTTP ${response.status} ao consultar a API.`);
    }
    return result;
  }

  function option(value, label) {
    const el = document.createElement('option');
    el.value = String(value);
    el.textContent = label;
    return el;
  }

  function fillOptions(select, items, placeholder, formatter) {
    const chosen = select.value;
    select.replaceChildren(option('', placeholder));
    items.forEach((item) => select.append(option(item.id, formatter(item))));
    if (Array.from(select.options).some((el) => el.value === chosen)) select.value = chosen;
    select.disabled = items.length === 0;
  }

  function getType(record) {
    if (MODALIDADES[record.tipo]) return record.tipo;
    if ('statusProcedimento' in record) return 'CIRURGICO';
    if ('statusEmergencia' in record) return 'EMERGENCIA';
    if ('especialidade' in record) return 'ESPECIALIZADA';
    return 'ROTINA';
  }

  function eligibleProfessional(p, tipo) {
    if (!p || p.ativo === false) return false;
    if (tipo === 'ROTINA') return p.tipo === 'CLINICO_GERAL';
    if (tipo === 'ESPECIALIZADA') return p.tipo === 'ESPECIALISTA';
    if (tipo === 'CIRURGICO') return p.tipo === 'CIRURGIAO';
    if (tipo === 'EMERGENCIA') return true;
    return false;
  }

  function updateSpeciality() {
    const prof = state.profissionais.find((p) => String(p.id) === byId('profissionalId').value);
    byId('especialidade').value = prof && prof.tipo === 'ESPECIALISTA' ? (prof.especialidade || '') : '';
  }

  function updateProfessionals() {
    const tipo = byId('tipo').value;
    const selected = state.profissionais.filter((p) => eligibleProfessional(p, tipo));
    const select = byId('profissionalId');
    fillOptions(select, selected, tipo ? 'Selecione um profissional...' : 'Selecione uma modalidade primeiro', (p) => {
      const area = p.tipo === 'ESPECIALISTA' ? p.especialidade : (p.tipo === 'CIRURGIAO' ? p.areaCirurgica : (p.tipo === 'CLINICO_GERAL' ? 'Clínica geral' : 'Saúde'));
      return area ? `${p.nome} · ${area}` : p.nome;
    });
    if (!tipo) select.disabled = true;
    byId('professional-help').textContent = !tipo ? 'A lista será filtrada conforme a modalidade.'
      : selected.length ? `${selected.length} profissional(is) ativo(s) compatível(is) com a modalidade.`
      : 'Nenhum profissional ativo compatível. Cadastre ou ative um profissional na API.';
    updateSpeciality();
    updateSubmitState();
  }

  function updateModality() {
    const tipo = byId('tipo').value;
    const fields = [
      ['motivo', tipo === 'ROTINA', true],
      ['especialidade', tipo === 'ESPECIALIZADA', true],
      ['prioridade', tipo === 'EMERGENCIA', true],
      ['tipoCirurgia', tipo === 'CIRURGICO', true],
      ['descricao', tipo === 'CIRURGICO', true],
      ['observacoesPreOperatorias', tipo === 'CIRURGICO', false]
    ];
    fields.forEach(([id, visible, required]) => {
      byId('field-' + id).hidden = !visible;
      byId(id).required = visible && required;
      if (!visible) byId(id).value = '';
    });
    // Apenas emergências aceitam data/hora passada na API.
    // As demais seguem a validação de data futura estabelecida no serviço Java.
    if (tipo !== 'EMERGENCIA') byId('dataHora').min = localDateTimeNow();
    else byId('dataHora').removeAttribute('min');
    updateProfessionals();
  }

  function localDateTimeNow() {
    const d = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  function updateSubmitState() {
    const ready = state.pacientes.length > 0 && !byId('pacienteId').disabled &&
      !byId('profissionalId').disabled && !state.busy;
    byId('submit-consulta').disabled = !ready;
  }

  async function loadPeople() {
    const results = await Promise.allSettled([request(API.pacientes), request(API.profissionais)]);
    if (results[0].status === 'fulfilled') {
      state.pacientes = Array.isArray(results[0].value) ? results[0].value : [];
      fillOptions(byId('pacienteId'), state.pacientes, 'Selecione um paciente...', (p) => p.nome || `Paciente #${p.id}`);
    } else {
      byId('pacienteId').replaceChildren(option('', 'Não foi possível carregar pacientes'));
      byId('pacienteId').disabled = true;
    }
    if (results[1].status === 'fulfilled') state.profissionais = Array.isArray(results[1].value) ? results[1].value : [];
    updateProfessionals();
    if (results.some((r) => r.status === 'rejected')) notify('Falha ao carregar pacientes ou profissionais. Verifique a API e clique em Atualizar.', true);
  }

  function createText(tag, cls, text) {
    const el = document.createElement(tag);
    if (cls) el.className = cls;
    el.textContent = text == null ? '—' : String(text);
    return el;
  }

  function formatDate(iso) {
    if (!iso) return { date: '—', time: '—' };
    const dt = new Date(iso);
    if (Number.isNaN(dt.getTime())) return { date: '—', time: '—' };
    return {
      date: new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(dt),
      time: new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(dt)
    };
  }

  function renderMetrics() {
    byId('metric-total').textContent = String(state.consultas.length);
    byId('metric-agendada').textContent = String(state.consultas.filter((c) => c.status === 'AGENDADA').length);
    byId('metric-atendimento').textContent = String(state.consultas.filter((c) => c.status === 'EM_ATENDIMENTO').length);
    byId('metric-concluida').textContent = String(state.consultas.filter((c) => c.status === 'CONCLUIDA').length);
  }

  function statusBadge(status) {
    const label = SITUACOES[status] || status || 'Desconhecida';
    const badge = createText('span', `sih-badge status--${Object.hasOwn(SITUACOES, status) ? status : 'NAO_COMPARECEU'}`, label);
    return badge;
  }

  function rowCell(primary, secondary, custom) {
    const cell = document.createElement('td');
    cell.append(createText('span', 'cell-strong', primary));
    if (secondary) cell.append(createText('span', 'cell-sub', secondary));
    if (custom) cell.classList.add('cell-ellipsis');
    return cell;
  }

  function placeholder(message, description, isError = false) {
    const row = document.createElement('tr');
    const td = document.createElement('td');
    td.colSpan = 6;
    const box = createText('div', 'table-placeholder', '');
    if (!isError) {
      const graphic = createText('div', 'placeholder-icon', '');
      graphic.innerHTML = '<svg class="icon"><use href="#i-calendar"/></svg>';
      box.append(graphic);
    }
    box.append(createText('strong', null, message));
    box.append(createText('p', null, description));
    td.append(box);
    row.append(td);
    byId('consultas-tbody').replaceChildren(row);
  }

  function matches(record, needle, filterStatus, filterType) {
    if (filterStatus && record.status !== filterStatus) return false;
    if (filterType && getType(record) !== filterType) return false;
    if (!needle) return true;
    const source = [record.paciente && record.paciente.nome, record.profissionalResponsavel && record.profissionalResponsavel.nome,
      record.motivo, record.descricao, record.tipoCirurgia, record.especialidade, MODALIDADES[getType(record)], SITUACOES[record.status]]
      .filter(Boolean).join(' ').toLocaleLowerCase('pt-BR');
    return source.includes(needle);
  }

  function renderTable() {
    if (!state.carregouAgenda) return;
    const needle = byId('filter-search').value.trim().toLocaleLowerCase('pt-BR');
    const status = byId('filter-status').value;
    const type = byId('filter-type').value;
    const results = state.consultas.filter((r) => matches(r, needle, status, type));
    const tbody = byId('consultas-tbody');
    tbody.replaceChildren();
    if (!results.length) {
      placeholder(state.consultas.length ? 'Nenhuma consulta encontrada' : 'Nenhuma consulta cadastrada',
        state.consultas.length ? 'Ajuste os filtros ou procure outro nome.' : 'Cadastre um atendimento no formulário acima para iniciar sua agenda.');
    } else {
      results.sort((a, b) => String(b.dataHora || '').localeCompare(String(a.dataHora || ''))).forEach((record) => {
        const tr = document.createElement('tr');
        const d = formatDate(record.dataHora);
        tr.append(rowCell(d.date, d.time));
        tr.append(rowCell(record.paciente && record.paciente.nome, record.paciente && record.paciente.id ? `Paciente #${record.paciente.id}` : null));
        tr.append(rowCell(MODALIDADES[getType(record)] || 'Atendimento',
          record.motivo || record.especialidade || record.descricao || (record.prioridade && `Prioridade: ${record.prioridade}`) || null));
        const prof = record.profissionalResponsavel || {};
        tr.append(rowCell(prof.nome || '—', prof.especialidade || prof.areaCirurgica || (prof.tipo === 'CLINICO_GERAL' ? 'Clínica geral' : null)));
        const statusCell = document.createElement('td');
        statusCell.append(statusBadge(record.status));
        tr.append(statusCell);
        const actionsCell = document.createElement('td');
        const button = createText('button', 'manage-button', 'Gerenciar');
        button.type = 'button';
        button.setAttribute('aria-label', `Gerenciar atendimento de ${record.paciente && record.paciente.nome || 'paciente'}`);
        button.addEventListener('click', () => openActions(record.id));
        actionsCell.append(button);
        tr.append(actionsCell);
        tbody.append(tr);
      });
    }
    byId('agenda-count').textContent = `Exibindo ${results.length} de ${state.consultas.length} registro(s)`;
  }

  async function loadConsultas() {
    const refresh = byId('refresh-consultas');
    refresh.disabled = true;
    byId('agenda-alert').hidden = true;
    try {
      const result = await request(API.consultas);
      if (!Array.isArray(result)) throw new Error('A API de consultas não retornou uma lista de registros.');
      state.consultas = result;
      state.carregouAgenda = true;
      renderMetrics();
      renderTable();
      apiIndicator('connected', 'API conectada');
    } catch (error) {
      state.carregouAgenda = false;
      apiIndicator('error', 'API indisponível');
      byId('agenda-alert').textContent = `${error.message} Verifique se o Spring Boot está em execução e abra esta página pelo endereço http://localhost:8080/consultas.html.`;
      byId('agenda-alert').hidden = false;
      placeholder('Não foi possível consultar a agenda', 'Use o botão Atualizar após restabelecer a conexão.', true);
      byId('agenda-count').textContent = 'Não foi possível carregar os registros';
      ['total', 'agendada', 'atendimento', 'concluida'].forEach((key) => byId('metric-' + key).textContent = '—');
    } finally {
      refresh.disabled = false;
    }
  }

  async function submitForm(event) {
    event.preventDefault();
    const form = byId('consulta-form');
    if (!form.reportValidity() || state.busy) return;
    const data = new FormData(form);
    const tipo = data.get('tipo');
    const body = {
      tipo, pacienteId: Number(data.get('pacienteId')), profissionalId: Number(data.get('profissionalId')),
      dataHora: data.get('dataHora')
    };
    if (tipo === 'ROTINA') body.motivo = String(data.get('motivo')).trim();
    if (tipo === 'ESPECIALIZADA') body.especialidade = String(data.get('especialidade')).trim();
    if (tipo === 'EMERGENCIA') body.prioridade = data.get('prioridade');
    if (tipo === 'CIRURGICO') {
      body.tipoCirurgia = String(data.get('tipoCirurgia')).trim();
      body.descricao = String(data.get('descricao')).trim();
      body.observacoesPreOperatorias = String(data.get('observacoesPreOperatorias')).trim() || null;
    }
    state.busy = true;
    updateSubmitState();
    try {
      await request(API.consultas, { method: 'POST', body: JSON.stringify(body) });
      notify('Atendimento cadastrado com sucesso.');
      form.reset();
      updateModality();
      await loadConsultas();
    } catch (error) { notify(error.message, true); }
    finally { state.busy = false; updateSubmitState(); }
  }

  function actionsAvailable(record) {
    const type = getType(record);
    const status = record.status;
    if (type === 'ROTINA' || type === 'ESPECIALIZADA') {
      const change = (label, to, destructive = false) => ({ label, destructive, url: `${API.consultas}/${record.id}/status`, body: { status: to } });
      if (status === 'AGENDADA') return [change('Confirmar consulta', 'CONFIRMADA'), change('Registrar ausência', 'NAO_COMPARECEU'), change('Cancelar consulta', 'CANCELADA', true)];
      if (status === 'CONFIRMADA') return [change('Iniciar atendimento', 'EM_ATENDIMENTO'), change('Registrar ausência', 'NAO_COMPARECEU'), change('Cancelar consulta', 'CANCELADA', true)];
      if (status === 'EM_ATENDIMENTO') return [change('Concluir consulta', 'CONCLUIDA'), change('Cancelar consulta', 'CANCELADA', true)];
      return [];
    }
    if (type === 'EMERGENCIA') {
      if (record.statusEmergencia === 'AGUARDANDO') return [{ label: 'Iniciar avaliação', url: `${API.consultas}/${record.id}/emergencia/iniciar` }];
      if (record.statusEmergencia === 'INICIADO') return [{ label: 'Concluir emergência', url: `${API.consultas}/${record.id}/emergencia/concluir` }];
      return [];
    }
    if (type === 'CIRURGICO') {
      const base = `${API.consultas}/${record.id}/cirurgia`;
      if (record.statusProcedimento === 'AGENDADO') return [
        { label: 'Iniciar procedimento', url: `${base}/iniciar` },
        { label: 'Cancelar procedimento', destructive: true, url: `${base}/cancelar` }
      ];
      if (record.statusProcedimento === 'EM_ANDAMENTO') return [
        { label: 'Concluir procedimento', url: `${base}/concluir` },
        { label: 'Cancelar procedimento', destructive: true, url: `${base}/cancelar` }
      ];
    }
    return [];
  }

  function addDetail(wrapper, key, value) {
    const cell = document.createElement('div');
    cell.append(createText('span', 'dialog-detail-label', key));
    cell.append(createText('span', 'dialog-detail-value', value));
    wrapper.append(cell);
  }

  function isObservationAllowed(record) {
    return record.status === 'CONCLUIDA' && ['ROTINA', 'ESPECIALIZADA', 'CIRURGICO'].includes(getType(record));
  }

  function openActions(id) {
    if (state.busy) return;
    const record = state.consultas.find((r) => String(r.id) === String(id));
    if (!record) return;
    state.selecionada = record;
    byId('dialog-title').textContent = `Atendimento #${record.id}`;
    const details = byId('dialog-details');
    details.replaceChildren();
    const d = formatDate(record.dataHora);
    addDetail(details, 'Paciente', record.paciente && record.paciente.nome || '—');
    addDetail(details, 'Profissional', record.profissionalResponsavel && record.profissionalResponsavel.nome || '—');
    addDetail(details, 'Modalidade', MODALIDADES[getType(record)]);
    addDetail(details, 'Data e horário', `${d.date} às ${d.time}`);
    addDetail(details, 'Situação', SITUACOES[record.status] || '—');
    if (record.prioridade) addDetail(details, 'Prioridade', record.prioridade);
    if (record.descricao || record.motivo) addDetail(details, 'Descrição', record.descricao || record.motivo);
    if (record.tipoCirurgia) addDetail(details, 'Tipo de cirurgia', record.tipoCirurgia);
    if (record.dataHoraEfetiva) { const dEfetiva = formatDate(record.dataHoraEfetiva); addDetail(details, 'Realização efetiva', `${dEfetiva.date} às ${dEfetiva.time}`); }
    if (record.statusEmergencia) addDetail(details, 'Etapa da emergência', ({ AGUARDANDO: 'Aguardando avaliação', INICIADO: 'Avaliação iniciada', CONCLUIDO: 'Avaliação concluída' })[record.statusEmergencia] || record.statusEmergencia);
    if (record.statusProcedimento) addDetail(details, 'Etapa cirúrgica', ({ AGENDADO: 'Agendado', EM_ANDAMENTO: 'Em andamento', CONCLUIDO: 'Concluído', CANCELADO: 'Cancelado' })[record.statusProcedimento] || record.statusProcedimento);
    const buttons = byId('dialog-actions');
    buttons.replaceChildren();
    const actions = actionsAvailable(record);
    actions.forEach((action) => {
      const button = createText('button', `dialog-action-btn${action.destructive ? ' danger' : ''}`, action.label);
      button.type = 'button';
      button.addEventListener('click', () => applyAction(action));
      buttons.append(button);
    });
    if (!actions.length && !isObservationAllowed(record)) buttons.append(createText('span', 'dialog-empty', 'Nenhuma alteração permitida para a situação atual.'));
    else if (!actions.length) buttons.append(createText('span', 'dialog-empty', 'Atendimento finalizado. É possível registrar ou atualizar as observações.'));
    const obsForm = byId('observacoes-form');
    obsForm.hidden = !isObservationAllowed(record);
    if (!obsForm.hidden) {
      const surgery = getType(record) === 'CIRURGICO';
      byId('observacoes-label').textContent = surgery ? 'Observações pós-operatórias' : 'Observações clínicas após a consulta';
      byId('observacoes-dialog').value = surgery ? (record.observacoesPosOperatorias || '') : (record.observacoes || '');
    }
    byId('consulta-dialog').showModal();
  }

  async function applyAction(action) {
    if (!state.selecionada || state.busy) return;
    if (action.destructive && !window.confirm(`Confirma a ação: ${action.label.toLocaleLowerCase('pt-BR')}?`)) return;
    state.busy = true;
    byId('dialog-actions').querySelectorAll('button').forEach((b) => b.disabled = true);
    try {
      await request(action.url, { method: 'PATCH', ...(action.body ? { body: JSON.stringify(action.body) } : {}) });
      byId('consulta-dialog').close();
      notify('Situação atualizada com sucesso.');
      await loadConsultas();
    } catch (error) { notify(error.message, true); }
    finally { state.busy = false; byId('dialog-actions').querySelectorAll('button').forEach((b) => b.disabled = false); }
  }

  async function submitObservations(event) {
    event.preventDefault();
    const record = state.selecionada;
    if (!record || !isObservationAllowed(record) || state.busy) return;
    const texto = byId('observacoes-dialog').value.trim();
    if (!texto) { notify('Preencha as observações antes de salvar.', true); return; }
    const isSurgery = getType(record) === 'CIRURGICO';
    const route = `${API.consultas}/${record.id}/${isSurgery ? 'cirurgia/pos-operatorio' : 'observacoes'}`;
    state.busy = true;
    byId('submit-observacoes').disabled = true;
    try {
      await request(route, { method: 'PATCH', body: JSON.stringify({ observacoes: texto }) });
      byId('consulta-dialog').close();
      notify('Observações registradas com sucesso.');
      await loadConsultas();
    } catch (error) { notify(error.message, true); }
    finally { state.busy = false; byId('submit-observacoes').disabled = false; }
  }

  document.addEventListener('DOMContentLoaded', function () {
    byId('consulta-form').addEventListener('submit', submitForm);
    byId('consulta-form').addEventListener('reset', () => setTimeout(updateModality, 0));
    byId('tipo').addEventListener('change', updateModality);
    byId('profissionalId').addEventListener('change', updateSpeciality);
    byId('filter-search').addEventListener('input', renderTable);
    byId('filter-status').addEventListener('change', renderTable);
    byId('filter-type').addEventListener('change', renderTable);
    byId('refresh-consultas').addEventListener('click', async () => { await Promise.all([loadPeople(), loadConsultas()]); });
    byId('close-dialog').addEventListener('click', () => byId('consulta-dialog').close());
    byId('consulta-dialog').addEventListener('click', (event) => { if (event.target === byId('consulta-dialog')) byId('consulta-dialog').close(); });
    byId('observacoes-form').addEventListener('submit', submitObservations);
    updateModality();
    Promise.all([loadPeople(), loadConsultas()]);
  });
}());
