package br.com.banco.aberturaconta.infra.mapper;

import br.com.banco.aberturaconta.core.model.SolicitacaoAberturaConta;
import br.com.banco.aberturaconta.infra.entity.SolicitacaoAberturaContaData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SolicitacaoAberturaContaMapper {
    
    SolicitacaoAberturaContaMapper INSTANCE = Mappers.getMapper(SolicitacaoAberturaContaMapper.class);

    SolicitacaoAberturaConta toModel(SolicitacaoAberturaContaData data);

    SolicitacaoAberturaContaData toData(SolicitacaoAberturaConta model);
}