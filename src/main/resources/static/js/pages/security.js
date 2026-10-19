import { get, put } from '../core/api.js';
import { paginacao, painel } from '../core/componentes.js';
import { carregando, el, icone, substituir } from '../core/dom.js';
import { dataHora, EVENTOS } from '../core/formato.js';
import { avisar, comBotaoOcupado, iniciarPagina, limparErros, mostrarErro, sair } from '../core/layout.js';

const usuario = await iniciarPagina({ protegida: true, trilha: [{ texto: 'Painel', href: '/dashboard' }, { texto: 'Segurança' }] });
const eventos = el('div', {}, carregando());

substituir('pagina',
    el('div', { class: 'tb-cabecalho-pagina' }, el('div', {},
        el('h1', { text: 'Segurança da conta' }),
        el('p', { text: 'Senha, e-mail de acesso e o histórico de eventos da sua conta.' }))),
    el('div', { class: 'row g-4' },
        el('div', { class: 'col-lg-6' }, painelSenha(), painelEmail()),
        el('div', { class: 'col-lg-6' }, painelSessao(), painelLoginSocial(), painel('Eventos recentes', eventos))));

carregarEventos(0);

function formulario(campos, rotulo, aoEnviar) {
    const form = el('form', { class: 'tb-painel-corpo', novalidate: true },
        el('div', { class: 'alert alert-danger', 'data-erro-formulario': true, tabindex: '-1', hidden: true }),
        campos,
        el('button', { class: 'btn btn-primary', type: 'submit', text: rotulo }));
    form.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        limparErros(form);
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        await comBotaoOcupado(form.querySelector('[type=submit]'), async () => {
            try {
                await aoEnviar(new FormData(form));
                form.reset();
                carregarEventos(0);
            } catch (erro) {
                mostrarErro(form, erro);
            }
        });
    });
    return form;
}

function campo(rotulo, atributos) {
    const id = 'seg-' + atributos.name;
    return el('div', { class: 'mb-3' },
        el('label', { class: 'form-label', for: id, text: rotulo }),
        el('input', { class: 'form-control', id, ...atributos }));
}

function painelSenha() {
    if (!usuario.possuiSenha) {
        // Conta criada pelo Google/GitHub: define a primeira senha sem "senha atual".
        return painel('Definir senha', formulario([
            el('p', { class: 'small', text: 'Sua conta foi criada com login social e ainda não tem senha. Defina uma para também poder entrar com e-mail e senha.' }),
            campo('Nova senha', { name: 'novaSenha', type: 'password', autocomplete: 'new-password', required: true, minlength: 10, maxlength: 64 }),
            el('p', { class: 'form-text mt-n2 mb-3', text: 'Mínimo de 10 caracteres, com letras e números.' }),
        ], 'Definir senha', async (dados) => {
            await put('/api/me/senha', { senhaAtual: null, novaSenha: dados.get('novaSenha') });
            avisar('Senha definida.');
            window.location.reload();
        }));
    }
    return painel('Alterar senha', formulario([
        campo('Senha atual', { name: 'senhaAtual', type: 'password', autocomplete: 'current-password', required: true, maxlength: 128 }),
        campo('Nova senha', { name: 'novaSenha', type: 'password', autocomplete: 'new-password', required: true, minlength: 10, maxlength: 64 }),
        el('p', { class: 'form-text mt-n2 mb-3', text: 'Mínimo de 10 caracteres, com letras e números.' }),
    ], 'Alterar senha', async (dados) => {
        await put('/api/me/senha', { senhaAtual: dados.get('senhaAtual'), novaSenha: dados.get('novaSenha') });
        avisar('Senha alterada.');
    }));
}

function painelLoginSocial() {
    const corpo = el('div', { class: 'tb-painel-corpo small' }, el('div', { class: 'tb-carregando', text: 'Carregando…' }));
    get('/api/me/login-social').then((vinculados) => {
        const nomes = { GOOGLE: 'Google', GITHUB: 'GitHub' };
        substituir(corpo,
            vinculados.length
                ? el('ul', { class: 'list-unstyled mb-2' }, vinculados.map((provedor) => el('li', { class: 'd-flex gap-2 align-items-center mb-1' },
                    el('img', { src: `/img/redes/${provedor.toLowerCase()}.svg`, alt: '', width: 16, height: 16 }),
                    el('span', { text: `${nomes[provedor]} vinculado` }))))
                : el('p', { class: 'mb-2', text: 'Nenhuma conta vinculada.' }),
            el('p', { class: 'text-secondary mb-0', text: 'Para vincular, saia e use "Continuar com Google/GitHub" na tela de login com o mesmo e-mail desta conta (verificado no provedor).' }));
    }).catch(() => substituir(corpo, el('p', { class: 'text-danger mb-0', text: 'Não foi possível carregar.' })));
    return painel('Login com Google / GitHub', corpo);
}

function painelEmail() {
    if (!usuario.possuiSenha) {
        return painel('E-mail de acesso', el('div', { class: 'tb-painel-corpo small' },
            el('p', {}, 'Atual: ', el('strong', { text: usuario.email })),
            el('p', { class: 'text-secondary mb-0', text: 'Defina uma senha antes de alterar o e-mail: ela confirma que é você quem está pedindo a troca.' })));
    }
    return painel('E-mail de acesso', formulario([
        el('p', { class: 'small' }, 'Atual: ', el('strong', { text: usuario.email })),
        campo('Novo e-mail', { name: 'novoEmail', type: 'email', autocomplete: 'email', required: true, maxlength: 160 }),
        campo('Confirme com sua senha', { name: 'senhaAtual', type: 'password', autocomplete: 'current-password', required: true, maxlength: 128 }),
    ], 'Alterar e-mail', async (dados) => {
        const atualizado = await put('/api/me/email', { novoEmail: dados.get('novoEmail'), senhaAtual: dados.get('senhaAtual') });
        avisar('E-mail alterado para ' + atualizado.email + '.');
    }));
}

function painelSessao() {
    return painel('Sessão', el('div', { class: 'tb-painel-corpo' },
        el('p', { class: 'small', text: 'A sessão expira após 30 minutos sem uso. Após várias tentativas de login com senha errada, a conta fica bloqueada por 15 minutos.' }),
        el('button', { class: 'btn btn-outline-secondary', type: 'button', on: { click: sair } }, icone('log-out'), 'Encerrar sessão')));
}

async function carregarEventos(pagina) {
    const resultado = await get(`/api/me/eventos-seguranca?page=${pagina}&size=10`);
    substituir(eventos, el('div', { class: 'table-responsive' }, el('table', { class: 'table table-sm' },
        el('thead', {}, el('tr', {}, el('th', { text: 'Evento' }), el('th', { text: 'Quando' }), el('th', { text: 'IP' }))),
        el('tbody', {}, resultado.conteudo.map((evento) => el('tr', {},
            el('td', { text: EVENTOS[evento.evento] || evento.evento.toLowerCase().replaceAll('_', ' ') }),
            el('td', { class: 'text-nowrap', text: dataHora(evento.criadoEm) }),
            el('td', { class: 'tb-mono small', text: evento.ip || '—' })))))),
    paginacao(resultado, carregarEventos));
}
