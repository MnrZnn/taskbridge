import { post } from '../core/api.js';
import { comBotaoOcupado, iniciarPagina, mostrarErro } from '../core/layout.js';
import { montarLoginSocial } from '../core/social.js';

const usuario = await iniciarPagina({ trilha: [{ texto: 'Início', href: '/' }, { texto: 'Criar conta' }] });
if (usuario) window.location.replace('/dashboard');

const formulario = document.getElementById('form-registro');
montarLoginSocial('login-social');
if (new URLSearchParams(window.location.search).get('tipo') === 'PRESTADOR') {
    formulario.querySelector('#tipo-prestador').checked = true;
}

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const dados = new FormData(formulario);
    const corpo = {
        nomeCompleto: dados.get('nomeCompleto'),
        nomeUsuario: dados.get('nomeUsuario'),
        email: dados.get('email'),
        senha: dados.get('senha'),
        tipoConta: dados.get('tipoConta'),
        aceiteTermos: formulario.aceiteTermos.checked,
    };
    await comBotaoOcupado(formulario.querySelector('[type=submit]'), async () => {
        try {
            await post('/api/auth/registro', corpo);
            await post('/api/auth/login', { email: corpo.email, senha: corpo.senha });
            window.location.assign(corpo.tipoConta === 'PRESTADOR' ? '/settings?secao=profissional' : '/projects/new');
        } catch (erro) {
            mostrarErro(formulario, erro);
        }
    });
});
