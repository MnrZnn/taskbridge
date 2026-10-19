import { get, post } from '../core/api.js';
import { painel } from '../core/componentes.js';
import { el, substituir } from '../core/dom.js';
import { comBotaoOcupado, iniciarPagina, mostrarErro } from '../core/layout.js';

await iniciarPagina({ trilha: [{ texto: 'Início', href: '/' }, { texto: 'Concluir cadastro' }] });

let pendente;
try {
    pendente = await get('/api/auth/cadastro-social');
} catch {
    substituir('pagina', el('div', { class: 'tb-caixa-auth' }, el('div', { class: 'alert alert-warning' },
        'Não há cadastro em andamento (ou ele expirou). ', el('a', { href: '/login', text: 'Entrar novamente' }), '.')));
    throw new Error('sem cadastro pendente');
}

const opcaoConta = (valor, id, titulo, descricao, marcada) => el('div', { class: 'tb-opcao-conta' },
    el('input', { type: 'radio', name: 'tipoConta', id, value: valor, checked: marcada }),
    el('label', { for: id }, el('strong', { text: titulo }), el('small', { text: descricao })));

const formulario = el('form', { class: 'tb-painel-corpo', novalidate: true },
    el('div', { class: 'alert alert-danger', 'data-erro-formulario': true, tabindex: '-1', hidden: true }),
    el('p', {}, `Você entrou com o ${pendente.provedorRotulo} como `, el('strong', { text: pendente.email }),
        '. Falta pouco: escolha como vai usar o TaskBridge e o seu nome público.'),
    el('fieldset', { class: 'mb-3' }, el('legend', { text: 'Como você vai usar o TaskBridge?' }),
        el('div', { class: 'tb-opcoes-conta' },
            opcaoConta('CLIENTE', 'tipo-cliente', 'Quero contratar', 'Descrevo necessidades e recebo propostas.', true),
            opcaoConta('PRESTADOR', 'tipo-prestador', 'Quero trabalhar', 'Ofereço serviços e envio propostas.', false))),
    el('div', { class: 'row g-3' },
        el('div', { class: 'col-md-7' },
            el('label', { class: 'form-label', for: 'nomeCompleto', text: 'Nome completo' }),
            el('input', { class: 'form-control', id: 'nomeCompleto', name: 'nomeCompleto', required: true, minlength: 3, maxlength: 120, value: pendente.nomeSugerido })),
        el('div', { class: 'col-md-5' },
            el('label', { class: 'form-label', for: 'nomeUsuario', text: 'Nome de usuário' }),
            el('input', { class: 'form-control', id: 'nomeUsuario', name: 'nomeUsuario', required: true, pattern: '[A-Za-z0-9_.]{3,30}', maxlength: 30, value: pendente.usuarioSugerido }),
            el('div', { class: 'form-text', text: 'Público. Letras, números, ponto e _.' }))),
    el('div', { class: 'form-check mt-3 mb-3' },
        el('input', { class: 'form-check-input', type: 'checkbox', id: 'aceiteTermos', name: 'aceiteTermos', required: true }),
        el('label', { class: 'form-check-label small', for: 'aceiteTermos' }, 'Li e aceito os ',
            el('a', { href: '/terms', target: '_blank', rel: 'noopener', text: 'Termos de uso' }), ' e a ',
            el('a', { href: '/privacy', target: '_blank', rel: 'noopener', text: 'Política de privacidade' }), '.')),
    el('button', { class: 'btn btn-primary w-100', type: 'submit', text: 'Concluir cadastro' }));

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    if (!formulario.checkValidity()) {
        formulario.reportValidity();
        return;
    }
    const dados = new FormData(formulario);
    await comBotaoOcupado(formulario.querySelector('[type=submit]'), async () => {
        try {
            const usuario = await post('/api/auth/cadastro-social', {
                nomeCompleto: dados.get('nomeCompleto'),
                nomeUsuario: dados.get('nomeUsuario'),
                tipoConta: dados.get('tipoConta'),
                aceiteTermos: formulario.aceiteTermos.checked,
            });
            window.location.assign(usuario.tipoConta === 'PRESTADOR' ? '/settings?secao=profissional' : '/projects/new');
        } catch (erro) {
            mostrarErro(formulario, erro);
        }
    });
});

substituir('pagina', el('div', { class: 'tb-caixa-auth tb-larga' }, painel('Concluir cadastro', formulario)));
