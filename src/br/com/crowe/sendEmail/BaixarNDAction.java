package br.com.crowe.sendEmail;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.AgendamentoRelatorioHelper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;
import com.sankhya.util.SessionFile;
import com.sankhya.util.UIDGenerator;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BaixarNDAction implements AcaoRotinaJava {
  private static final String LINK_BAIXAR = "<a title=\"Visualizar Arquivo\" href=\"/mge/visualizadorArquivos.mge?chaveArquivo={0}\" target=\"_blank\"><u><b>{1}</b></u></a>";
  
  private static final BigDecimal NURFE_NOTA_DEBITO = new BigDecimal(26);
  
  public void doAction(ContextoAcao contexto) throws Exception {
    JdbcWrapper jdbc = null;
    
    try {
    	
      if ((contexto.getLinhas()).length > 1)
        throw new Exception("Selecione apenas uma linha!"); 
      ConcatenatePDF concatenate = new ConcatenatePDF();
      Registro linha = contexto.getLinhas()[0];
      EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
      jdbc = dwfEntityFacade.getJdbcWrapper();
      BigDecimal id = (BigDecimal)linha.getCampo("ID");
      AgendamentoRelatorioHelper.ParametroRelatorio pk = new AgendamentoRelatorioHelper.ParametroRelatorio("PK_ID", BigDecimal.class.getName(), id);
      List<Object> lstParam = new ArrayList();
      lstParam.add(pk);
      byte[] pdfBytes = AgendamentoRelatorioHelper.getPrintableReport(NURFE_NOTA_DEBITO, lstParam, contexto.getUsuarioLogado(), dwfEntityFacade);
      concatenate.addPdfFile(pdfBytes);
      
      NativeSql sqlAnexos = new NativeSql(jdbc);
      sqlAnexos.appendSql(" SELECT DET.ANEXO ");
      sqlAnexos.appendSql(" FROM AD_FATND FAT ");
      sqlAnexos.appendSql("   INNER JOIN AD_RDVFATND RDV ON FAT.ID = RDV.ID ");
      sqlAnexos.appendSql("   INNER JOIN AD_DETRDVFAT DET ON DET.ID = FAT.ID AND DET.RDV = RDV.RDV ");
      sqlAnexos.appendSql("   WHERE FAT.ID = ? ");
      sqlAnexos.appendSql("     AND DET.ANEXO IS NOT NULL");
      sqlAnexos.addParameter(id);
      
      ResultSet rsAnexos = sqlAnexos.executeQuery();
      
      while (rsAnexos.next())
        concatenate.addPdfFile(rsAnexos.getBytes(1)); 
      rsAnexos.close();
      ByteArrayOutputStream bytesPdf = concatenate.run();
      SessionFile fileReport = SessionFile.createSessionFile("Nota Debito.pdf", "application/pdf", bytesPdf.toByteArray());
      String chaveSessaoArquivo = UIDGenerator.getNextID();
      ServiceContext.getCurrent().putHttpSessionAttribute(chaveSessaoArquivo, (Serializable)fileReport);
      contexto.setMensagemRetorno(String.format("Arquivo Gerado.\nClique %s para visualizar.", new Object[] { getLinkBaixar("aqui", chaveSessaoArquivo) }));
      
      bytesPdf.close();
      
    } finally {
      JdbcWrapper.closeSession(jdbc);
    } 
  }
  
  private String getLinkBaixar(String descricao, String chave) {
    String url = "<a title=\"Visualizar Arquivo\" href=\"/mge/visualizadorArquivos.mge?chaveArquivo={0}\" target=\"_blank\"><u><b>{1}</b></u></a>".replace("{0}", chave);
    url = url.replace("{1}", descricao);
    return url;
  }
}
