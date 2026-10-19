import { post } from '../core/api.js';
import { comBotaoOcupado, iniciarPagina, mostrarErro } from '../core/layout.js';
import { ERROS_LOGIN_SOCIAL, montarLoginSocial } from '../core/social.js';

const usuario = await iniciarPagina({ trilha: [{ texto: 'Início', href: '/' }, { texto: 'Entrar' }] });

/** Só aceita destino interno: "?next=https://site-malicioso" viraria um open redirect. */
function destinoSeguro() {
    const next = new URLSearchParams(window.location.search).get('next');
    return next && next.startsWith('/') && !next.startsWith('//') && !next.startsWith('/\\') ? next : '/dashboard';
}

if (usuario) window.location.replace(destinoSeguro());

const formulario = document.getElementById('form-login');
montarLoginSocial('login-social');

// Mensagem vem de um mapa fixo pelo código da URL: nada da query string é exibido diretamente.
const erroSocial = ERROS_LOGIN_SOCIAL[new URLSearchParams(window.location.search).get('erro')];
if (erroSocial) {
    const alerta = formulario.querySelector('[data-erro-formulario]');
    alerta.textContent = erroSocial;
    alerta.hidden = false;
}
formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const dados = new FormData(formulario);
    await comBotaoOcupado(formulario.querySelector('[type=submit]'), async () => {
        try {
            await post('/api/auth/login', { email: dados.get('email'), senha: dados.get('senha') });
            window.location.assign(destinoSeguro());
        } catch (erro) {
            formulario.senha.value = '';
            mostrarErro(formulario, erro);
        }
    });
});
