import { get } from '../core/api.js';
import { seloCategoria } from '../core/componentes.js';
import { el, substituir } from '../core/dom.js';
import { iniciarPagina } from '../core/layout.js';

const usuario = await iniciarPagina();

if (usuario) {
    substituir('inicio-acoes',
        usuario.tipoConta === 'CLIENTE'
            ? el('a', { class: 'btn btn-primary btn-lg', href: '/projects/new', text: 'Descrever uma nova necessidade' })
            : el('a', { class: 'btn btn-primary btn-lg', href: '/projects?aba=compativeis', text: 'Ver projetos compatíveis' }),
        el('a', { class: 'btn btn-outline-secondary btn-lg', href: '/dashboard', text: 'Ir para o painel' }));
}

try {
    const categorias = await get('/api/categorias');
    substituir('inicio-categorias', categorias.map((categoria) => el('a', {
        class: 'tb-categoria text-decoration-none text-reset', href: `/services?categoriaId=${categoria.id}`,
    }, seloCategoria(categoria), el('div', {}, el('strong', { text: categoria.nome }), el('small', { text: categoria.descricao })))));
} catch {
    substituir('inicio-categorias', el('p', { class: 'p-3 text-secondary', text: 'Não foi possível carregar as categorias.' }));
}
