import { api, del, get, post, put } from '../core/api.js';
import { confirmar, painel } from '../core/componentes.js';
import { el, icone, renderizarIcones, substituir } from '../core/dom.js';
import { DISPONIBILIDADES, iniciais, MODALIDADES } from '../core/formato.js';
import { avisar, comBotaoOcupado, iniciarPagina, limparErros, mostrarErro } from '../core/layout.js';
import { iconeRede, identificarRede } from '../core/redes.js';

let usuario = await iniciarPagina({ protegida: true, trilha: [{ texto: 'Painel', href: '/dashboard' }, { texto: 'Configurações' }] });
const prestador = usuario.tipoConta === 'PRESTADOR';
const TAMANHO_MAXIMO_FOTO = 2 * 1024 * 1024;

const secoes = [
    { id: 'dados', rotulo: 'Dados pessoais', icone: 'user', desenhar: secaoDados },
    { id: 'foto', rotulo: 'Foto de perfil', icone: 'image', desenhar: secaoFoto },
    prestador ? { id: 'profissional', rotulo: 'Perfil profissional', icone: 'briefcase', desenhar: secaoProfissional } : null,
    { id: 'privacidade', rotulo: 'Privacidade e dados', icone: 'shield-check', desenhar: secaoPrivacidade },
].filter(Boolean);

const irParaLinks = window.location.hash === '#links';
const pedida = new URLSearchParams(window.location.search).get('secao');
let atual = secoes.some((secao) => secao.id === pedida) ? pedida : 'dados';
const menu = el('nav', { class: 'list-group list-group-flush', 'aria-label': 'Seções' });
const conteudo = el('div');

substituir('pagina',
    el('div', { class: 'tb-cabecalho-pagina' }, el('div', {},
        el('h1', { text: 'Configurações' }),
        el('p', {}, 'Seus dados e preferências. Senha e e-mail ficam em ', el('a', { href: '/security', text: 'Segurança' }), '.'))),
    el('div', { class: 'row g-4' },
        el('aside', { class: 'col-lg-3' }, el('section', { class: 'tb-painel' }, menu)),
        el('div', { class: 'col-lg-9' }, conteudo)));
mostrar(atual);
if (irParaLinks) document.getElementById('links')?.scrollIntoView();

function mostrar(id) {
    atual = id;
    history.replaceState(null, '', `/settings?secao=${id}`);
    substituir(menu, secoes.map((secao) => el('button', {
        type: 'button', class: 'list-group-item list-group-item-action d-flex gap-2 align-items-center' + (secao.id === id ? ' active' : ''),
        'aria-current': secao.id === id ? 'page' : null, on: { click: () => mostrar(secao.id) },
    }, icone(secao.icone), secao.rotulo)));
    substituir(conteudo, secoes.find((secao) => secao.id === id).desenhar());
}

function formulario(campos, aoEnviar, rotuloBotao = 'Salvar') {
    const form = el('form', { class: 'tb-painel-corpo', novalidate: true },
        el('div', { class: 'alert alert-danger', 'data-erro-formulario': true, tabindex: '-1', hidden: true }),
        campos,
        el('button', { class: 'btn btn-primary mt-2', type: 'submit', text: rotuloBotao }));
    form.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        limparErros(form);
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        await comBotaoOcupado(form.querySelector('[type=submit]'), async () => {
            try {
                await aoEnviar(new FormData(form), form);
            } catch (erro) {
                mostrarErro(form, erro);
            }
        });
    });
    return form;
}

function campo(rotulo, entrada, ajuda, classe = 'col-12') {
    entrada.id = entrada.id || 'cfg-' + entrada.name;
    return el('div', { class: classe },
        el('label', { class: 'form-label', for: entrada.id, text: rotulo }),
        entrada,
        ajuda ? el('div', { class: 'form-text', text: ajuda }) : null);
}

function textarea(nome, valor, maximo) {
    const area = el('textarea', { class: 'form-control', name: nome, rows: 4, maxlength: maximo });
    area.value = valor || '';
    return area;
}

// ------------------------------------------------------------------ dados pessoais

function secaoDados() {
    return el('div', {}, painelDados(), painelLinks());
}

/** Até 5 links; o ícone do site aparece enquanto a pessoa digita, igual ao que será mostrado no perfil. */
function painelLinks() {
    const lista = el('div');
    const adicionar = el('button', { class: 'btn btn-sm btn-outline-primary', type: 'button' }, icone('plus'), 'Adicionar link');
    const linha = (valor = '') => {
        const campo = el('input', { class: 'form-control', type: 'text', inputmode: 'url', autocomplete: 'url', name: 'link', maxlength: 300, value: valor,
            placeholder: 'https://www.linkedin.com/in/seu-perfil', 'aria-label': 'Endereço do link' });
        const icone_ = el('span', { class: 'tb-editor-link-icone', 'aria-hidden': 'true' });
        const atualizarIcone = () => {
            const texto = campo.value.trim();
            const rede = identificarRede(texto.includes('://') ? texto : 'https://' + texto);
            substituir(icone_, rede && texto ? iconeRede(rede) : icone('link'));
        };
        campo.addEventListener('input', atualizarIcone);
        atualizarIcone();
        const remover = el('button', { class: 'btn btn-sm btn-outline-danger', type: 'button', 'aria-label': 'Remover link' }, icone('trash-2'));
        const item = el('div', { class: 'tb-editor-link' }, icone_, campo, remover);
        remover.addEventListener('click', () => {
            item.remove();
            adicionar.disabled = lista.children.length >= 5;
        });
        return item;
    };
    const existentes = usuario.links.length ? usuario.links : [''];
    existentes.forEach((url) => lista.append(linha(url)));
    adicionar.disabled = lista.children.length >= 5;
    adicionar.addEventListener('click', () => {
        lista.append(linha());
        renderizarIcones(lista);
        adicionar.disabled = lista.children.length >= 5;
        lista.lastChild.querySelector('input').focus();
    });
    const form = formulario([
        el('p', { class: 'small text-secondary', text: 'Aparecem no canto do seu perfil público: LinkedIn, GitHub, portfólio ou qualquer outro site. Até 5.' }),
        lista,
        adicionar,
    ], async (dados, form) => {
        usuario = await put('/api/me/links', { links: dados.getAll('link').map((url) => String(url).trim()).filter(Boolean) });
        avisar('Links atualizados.');
        mostrar('dados');
    }, 'Salvar links');
    return painel('Links do perfil', form, { id: 'links' });
}

function painelDados() {
    return painel('Dados pessoais', formulario(el('div', { class: 'row g-3' },
        campo('Nome completo', el('input', { class: 'form-control', name: 'nomeCompleto', required: true, minlength: 3, maxlength: 120, value: usuario.nomeCompleto })),
        campo('Biografia', textarea('biografia', usuario.biografia, 1000), 'Aparece no seu perfil público. Não inclua documentos, telefone ou endereço.'),
        campo('Localização', el('input', { class: 'form-control', name: 'localizacao', maxlength: 80, value: usuario.localizacao, placeholder: 'Cidade, UF' }),
            'Opcional. Informe só cidade e estado.', 'col-md-6')),
    async (dados) => {
        usuario = await put('/api/me', {
            nomeCompleto: dados.get('nomeCompleto'),
            biografia: dados.get('biografia'),
            localizacao: dados.get('localizacao'),
        });
        avisar('Dados atualizados.');
    }));
}

// ------------------------------------------------------------------ foto

function secaoFoto() {
    const preview = usuario.fotoUrl
        ? el('img', { class: 'tb-foto-preview', src: usuario.fotoUrl, alt: 'Foto atual' })
        : el('div', { class: 'tb-perfil-foto-vazia', 'aria-hidden': 'true', text: iniciais(usuario.nomeCompleto) });
    const areaPreview = el('div', {}, preview);
    const entrada = el('input', { class: 'form-control', type: 'file', name: 'arquivo', id: 'arquivo', accept: 'image/jpeg,image/png', required: true });
    const alerta = el('div', { class: 'alert alert-danger', hidden: true, tabindex: '-1' });
    const enviar = el('button', { class: 'btn btn-primary', type: 'button', disabled: true }, icone('upload'), 'Enviar foto');
    let urlPreview;

    entrada.addEventListener('change', () => {
        alerta.hidden = true;
        const arquivo = entrada.files[0];
        if (urlPreview) URL.revokeObjectURL(urlPreview);
        if (!arquivo) return;
        // Verificação de conforto; a validação que vale (tipo real, dimensões, re-codificação) é no servidor.
        if (!['image/jpeg', 'image/png'].includes(arquivo.type) || arquivo.size > TAMANHO_MAXIMO_FOTO) {
            alerta.textContent = 'Envie uma imagem JPG ou PNG de até 2 MB.';
            alerta.hidden = false;
            enviar.disabled = true;
            return;
        }
        urlPreview = URL.createObjectURL(arquivo);
        const imagem = el('img', { class: 'tb-foto-preview', alt: 'Pré-visualização da nova foto' });
        imagem.src = urlPreview;
        substituir(areaPreview, imagem);
        enviar.disabled = false;
    });

    enviar.addEventListener('click', () => comBotaoOcupado(enviar, async () => {
        const dados = new FormData();
        dados.append('arquivo', entrada.files[0]);
        try {
            usuario = await api('/api/me/foto', { method: 'POST', formulario: dados });
            avisar('Foto atualizada.');
            mostrar('foto');
        } catch (erro) {
            alerta.textContent = erro.message;
            alerta.hidden = false;
        }
    }));

    const remover = usuario.fotoUrl ? el('button', { class: 'btn btn-outline-danger', type: 'button' }, icone('trash-2'), 'Remover foto') : null;
    remover?.addEventListener('click', () => comBotaoOcupado(remover, async () => {
        await del('/api/me/foto');
        usuario = await get('/api/me');
        avisar('Foto removida.');
        mostrar('foto');
    }));

    return painel('Foto de perfil', el('div', { class: 'tb-painel-corpo' },
        alerta,
        el('div', { class: 'd-flex flex-wrap gap-4 align-items-start' },
            areaPreview,
            el('div', { class: 'flex-grow-1' },
                el('label', { class: 'form-label', for: 'arquivo', text: 'Nova foto' }),
                entrada,
                el('div', { class: 'form-text mb-3', text: 'JPG ou PNG, até 2 MB. A imagem é recortada em formato quadrado e os metadados (como localização GPS) são removidos.' }),
                el('div', { class: 'd-flex gap-2' }, enviar, remover)))));
}

// ------------------------------------------------------------------ perfil profissional

function secaoProfissional() {
    const alvo = el('div', {}, el('div', { class: 'tb-carregando', text: 'Carregando…' }));
    get('/api/me/perfil-profissional')
        .catch((erro) => (erro.status === 404 ? null : Promise.reject(erro)))
        .then((perfil) => substituir(alvo, formularioProfissional(perfil)));
    return painel('Perfil profissional', alvo);
}

function formularioProfissional(perfil) {
    const opcoes = (mapa, selecionado) => Object.entries(mapa).map(([valor, rotulo]) => el('option', { value: valor, text: rotulo, selected: valor === selecionado }));
    const form = formulario(el('div', { class: 'row g-3' },
        perfil ? null : el('div', { class: 'col-12' }, el('div', { class: 'alert alert-info mb-0', text: 'Preencha para aparecer no matching dos projetos.' })),
        campo('Título profissional', el('input', { class: 'form-control', name: 'tituloProfissional', required: true, minlength: 3, maxlength: 100, value: perfil?.tituloProfissional, placeholder: 'Ex.: Desenvolvedor Web' }), null, 'col-md-8'),
        campo('Anos de experiência', el('input', { class: 'form-control', name: 'experienciaAnos', type: 'number', min: 0, max: 60, required: true, value: perfil?.experienciaAnos ?? 0 }), null, 'col-md-4'),
        campo('Descrição', textarea('descricao', perfil?.descricao, 2000)),
        campo('Disponibilidade', el('select', { class: 'form-select', name: 'disponibilidade', required: true }, opcoes(DISPONIBILIDADES, perfil?.disponibilidade || 'IMEDIATA')), null, 'col-md-6'),
        campo('Modalidade', el('select', { class: 'form-select', name: 'modalidadeTrabalho', required: true }, opcoes(MODALIDADES, perfil?.modalidadeTrabalho || 'REMOTO')), null, 'col-md-6'),
        campo('Preço mínimo por projeto (R$)', el('input', { class: 'form-control', name: 'faixaPrecoMinima', type: 'number', min: 0, step: '0.01', required: true, value: perfil?.faixaPrecoMinima }), null, 'col-md-6'),
        campo('Preço máximo por projeto (R$)', el('input', { class: 'form-control', name: 'faixaPrecoMaxima', type: 'number', min: 1, step: '0.01', required: true, value: perfil?.faixaPrecoMaxima }), null, 'col-md-6'),
        campo('Habilidades', el('input', { class: 'form-control', name: 'habilidades', maxlength: 600, value: perfil?.habilidades.join(', '), placeholder: 'java, spring, mysql' }),
            'Separe por vírgula (até 15). Comparadas com as habilidades pedidas nos projetos.')),
    async (dados) => {
        await put('/api/me/perfil-profissional', {
            tituloProfissional: dados.get('tituloProfissional'),
            descricao: dados.get('descricao') || null,
            experienciaAnos: Number(dados.get('experienciaAnos')),
            disponibilidade: dados.get('disponibilidade'),
            modalidadeTrabalho: dados.get('modalidadeTrabalho'),
            faixaPrecoMinima: Number(dados.get('faixaPrecoMinima')),
            faixaPrecoMaxima: Number(dados.get('faixaPrecoMaxima')),
            habilidades: String(dados.get('habilidades')).split(',').map((texto) => texto.trim()).filter(Boolean),
        });
        avisar('Perfil profissional salvo.');
    });
    return form;
}

// ------------------------------------------------------------------ LGPD

function secaoPrivacidade() {
    const exportar = el('button', { class: 'btn btn-outline-secondary', type: 'button' }, icone('download'), 'Baixar meus dados (JSON)');
    exportar.addEventListener('click', () => comBotaoOcupado(exportar, async () => {
        const dados = await get('/api/me/dados');
        const arquivo = new Blob([JSON.stringify(dados, null, 2)], { type: 'application/json' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(arquivo);
        link.download = 'taskbridge-meus-dados.json';
        link.click();
        URL.revokeObjectURL(link.href);
    }));

    const exclusao = formulario(el('div', { class: 'row g-3' },
        // Contas criadas pelo Google/GitHub sem senha própria confirmam só digitando EXCLUIR.
        usuario.possuiSenha ? campo('Senha atual', el('input', { class: 'form-control', type: 'password', name: 'senhaAtual', autocomplete: 'current-password', required: true, maxlength: 128 }), null, 'col-md-6') : null,
        campo('Digite EXCLUIR para confirmar', el('input', { class: 'form-control', name: 'confirmacao', required: true, pattern: 'EXCLUIR', autocomplete: 'off' }), null, 'col-md-6')),
    async (dados) => {
        const ok = await confirmar({ titulo: 'Excluir conta', mensagem: 'Esta ação não pode ser desfeita. Seus dados pessoais serão removidos e você será desconectado.', confirmar: 'Excluir minha conta', perigo: true });
        if (!ok) return;
        await post('/api/me/exclusao', { senhaAtual: dados.get('senhaAtual') || null, confirmacao: dados.get('confirmacao') });
        window.location.assign('/');
    }, 'Excluir minha conta');
    exclusao.querySelector('[type=submit]').className = 'btn btn-danger mt-2';

    return el('div', {},
        painel('Seus dados (LGPD)', el('div', { class: 'tb-painel-corpo' },
            el('p', { text: 'Você pode baixar tudo o que o TaskBridge guarda sobre você: conta, perfil, serviços, projetos, propostas, contratações e eventos de segurança.' }),
            exportar,
            el('p', { class: 'small text-secondary mt-3 mb-0' }, 'A versão dos termos que você aceitou e a data do aceite também estão no arquivo. ',
                el('a', { href: '/privacy', text: 'Política de privacidade' }), '.'))),
        painel('Excluir conta', el('div', {},
            el('div', { class: 'tb-painel-corpo pb-0' },
                el('p', { class: 'mb-2', text: 'Ao excluir a conta:' }),
                el('ul', { class: 'small' },
                    el('li', { text: 'nome, usuário, e-mail, biografia, localização e foto são apagados ou anonimizados;' }),
                    el('li', { text: 'perfil profissional, serviços, rascunhos e notificações são apagados;' }),
                    el('li', { text: 'projetos abertos são cancelados e propostas pendentes, canceladas;' }),
                    el('li', { text: 'contratações concluídas e pagamentos são mantidos, sem seus dados pessoais, pelo prazo exigido por obrigações legais.' })),
                el('p', { class: 'small text-secondary', text: 'Não é possível excluir a conta com contratações em andamento.' })),
            exclusao), { classe: 'border-danger-subtle' }));
}
