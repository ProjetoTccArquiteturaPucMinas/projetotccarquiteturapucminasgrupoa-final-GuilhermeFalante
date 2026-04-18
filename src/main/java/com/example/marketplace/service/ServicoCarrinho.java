package com.example.marketplace.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.marketplace.model.CategoriaProduto;
import com.example.marketplace.model.ItemCarrinho;
import com.example.marketplace.model.Produto;
import com.example.marketplace.model.ResumoCarrinho;
import com.example.marketplace.model.SelecaoCarrinho;
import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {

    private static final BigDecimal DESCONTO_MAXIMO = new BigDecimal("25");

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {

        List<ItemCarrinho> itens = new ArrayList<>();

        // =========================
        // Monta os itens do carrinho
        // =========================
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }

        // =========================
        // Calcula subtotal
        // =========================
        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int quantidadeTotalItens = itens.stream()
            .mapToInt(ItemCarrinho::getQuantidade)
            .sum();

        BigDecimal percentualQuantidade = descontoPorQuantidade(quantidadeTotalItens);

        BigDecimal percentualCategoria = itens.stream()
                .map(item -> descontoPorCategoria(item.getProduto().getCategoria())
                        .multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentualDesconto = percentualQuantidade.add(percentualCategoria)
            .min(DESCONTO_MAXIMO);

        BigDecimal valorDesconto = subtotal.multiply(percentualDesconto)
                .divide(new BigDecimal("100"));

        BigDecimal total = subtotal.subtract(valorDesconto);

        return new ResumoCarrinho(itens, subtotal, percentualDesconto, valorDesconto, total);
    }

    private BigDecimal descontoPorQuantidade(int quantidadeTotalItens) {
        if (quantidadeTotalItens <= 1) {
            return BigDecimal.ZERO;
        }

        if (quantidadeTotalItens == 2) {
            return new BigDecimal("5");
        }

        if (quantidadeTotalItens == 3) {
            return new BigDecimal("7");
        }

        return new BigDecimal("10");
    }

    private BigDecimal descontoPorCategoria(CategoriaProduto categoria) {
        return switch (categoria) {
            case CAPINHA, FONE -> new BigDecimal("3");
            case CARREGADOR -> new BigDecimal("5");
            case PELICULA, SUPORTE -> new BigDecimal("2");
        };
    }
}
