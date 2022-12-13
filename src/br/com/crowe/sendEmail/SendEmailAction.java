package br.com.crowe.sendEmail;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.AgendamentoRelatorioHelper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sankhya.util.SessionFile;
import com.sankhya.util.UIDGenerator;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SendEmailAction implements AcaoRotinaJava {
	
  BigDecimal codFila;
  BigDecimal codMsg;
  BigDecimal dtEntrada;
  BigDecimal status;
  BigDecimal tenteEnvio;
  BigDecimal codProj;
  BigDecimal rdv;
  BigDecimal nuAnexo;
  BigDecimal nuAnexo1;
  
  String assunto;
  String mensagem;
  String msg;
  String emailParc;
  String assuntoEmail;
  String query;
  String query2;
  String queryNuanexo;
  String coduso;
  String emailFornecedor = "tassio.vasconcelos@covenantit.com.br";
  FinderWrapper finde;
  
  byte[] anexo;
  byte[] pdfBytes;
  
  Gson GSON = (new GsonBuilder()).serializeNulls().create();
  
  public void doAction(ContextoAcao ctx) throws Exception {
	  
    JdbcWrapper JDBC = JapeFactory.getEntityFacade().getJdbcWrapper();
    NativeSql nativeSql = new NativeSql(JDBC);
    JapeSession.SessionHandle hnd = JapeSession.open();
    
   
    try {
    	
      System.out.println("Sysout Entrou no try depois do rel");
      
      for (int i = 0; i < (ctx.getLinhas()).length; i++) { 	  
        Registro linha = ctx.getLinhas()[i];
        
        BigDecimal codParc = (BigDecimal)linha.getCampo("CODPARC");
        BigDecimal codProj = (BigDecimal)linha.getCampo("CODPROJ");
        BigDecimal rdv = (BigDecimal)linha.getCampo("RDV");
        
        this.query = " SELECT EMAIL FROM TGFPAR WHERE CODPARC = " + codParc;
        System.out.println("sysout String Rs : " + this.query);
        ResultSet rs = nativeSql.executeQuery(this.query);
        System.out.println("SYSOUT RDV : " + rdv);
        System.out.println("Sysout depois do for captura de tela");
        
        while (rs.next()) {
          this.emailParc = rs.getString("EMAIL");
          System.out.println("SYSOUT EMAIL " + this.emailParc);
          System.out.println("sysout parceiro " + codParc);
          System.out.println("sysout Entrou no while");
        } 
        
        if (this.emailParc == null) {
          ctx.setMensagemRetorno("Parceiro sem email! \n Favor Cadastrar Email.");
          return;
        } 
        
        this.assuntoEmail = "Prezado(a) cliente,\r\n\r\nSegue anexa Nota de DNXX, com vencimento em , "
        		+ "com suas respectivas prestade contas e comprovantes, referente as despesas "
        		+ "incorridas no perno projeto 069/21CUSTOS.\r\n\r\n\r\n\r\nRazSocial: "
        		+ "CROWE MACRO AUDITORES E CONSULTORES LTDA\r\n\r\nCNPJ (PIX): "
        		+ "16.454.568/0001-12\r\n\r\nBanco: 341 - BANCO ITAU\r\n\r\nAg2954\r\n\r\nConta Corrente: "
        		+ "13885 - 1\r\n\r\n\r\n\r\nFicamos disposipara esclarecer quaisquer duvida." ;
        	char[] assuntoEmailchar = this.assuntoEmail.toCharArray();
        	
        this.emailFornecedor = "tassio.vasconcelos@covenantit.com.br";
        System.out.println("Sysout assunto : " + assuntoEmailchar);
        
        try {
        	
          System.out.println("SYSOUT ENTROU NO TRY");
          EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
          DynamicVO dynamicVO1 = (DynamicVO)dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
          
          System.out.println(this.codFila);
          
          dynamicVO1.setProperty("ASSUNTO", "TESTE");
          dynamicVO1.setProperty("CODMSG", null);
          dynamicVO1.setProperty("DTENTRADA", new Timestamp(System.currentTimeMillis()));
          dynamicVO1.setProperty("STATUS", "Pendente");
          dynamicVO1.setProperty("CODCON", new BigDecimal(0));
          dynamicVO1.setProperty("TENTENVIO", new BigDecimal(0));
          dynamicVO1.setProperty("MENSAGEM", assuntoEmailchar);
          System.out.println(assuntoEmailchar);
          dynamicVO1.setProperty("TIPOENVIO", "E");
          dynamicVO1.setProperty("MAXTENTENVIO", new BigDecimal(3));
          dynamicVO1.setProperty("EMAIL", "t.santos.vasconcelos@gmail.com");
          dynamicVO1.setProperty("CODSMTP", new BigDecimal(2));
          dynamicVO1.setProperty("CODUSUREMET", this.coduso);
          PersistentLocalEntity createEntity = dwfFacade.createEntity("MSDFilaMensagem", (EntityVO)dynamicVO1);
          
          DynamicVO save = (DynamicVO)createEntity.getValueObject();
          
          this.codFila = save.asBigDecimal("CODFILA");
          
          System.out.println("CODFILA Dentro da inclusao do email " + this.codFila);
          ctx.setMensagemRetorno("Email enviado com sucesso");
          
        } catch (Exception e) {
          e.printStackTrace();
          this.msg = "Erro na inclusao do item " + e.getMessage();
          System.out.println(this.msg);
        } 
        
        BigDecimal nuRfe = new BigDecimal(26);
        List<Object> lstParam = new ArrayList();
        
        byte[] pdfBytes = (byte[])null;
        
        String chave = "chave.pdf";
        
        Registro[] regs = ctx.getLinhas();
        
        try {
        	
          System.out.println("entrou no try do rel");
          EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
          AgendamentoRelatorioHelper.ParametroRelatorio pk = new AgendamentoRelatorioHelper.ParametroRelatorio("PK_ID", BigDecimal.class.getName(), regs[0].getCampo("ID"));
          lstParam.add(pk);
          System.out.println(pk);
          pdfBytes = AgendamentoRelatorioHelper.getPrintableReport(nuRfe, lstParam, ctx.getUsuarioLogado(), dwfFacade);
          SessionFile sessionFile = SessionFile.createSessionFile("Nota_Debito.pdf", "Nota_Debito", pdfBytes);
          ServiceContext.getCurrent().putHttpSessionAttribute(chave, (Serializable)sessionFile);
          System.out.println(pdfBytes);
          System.out.println("GErando anexo pdfbyte " + pdfBytes);
          
        } catch (Exception e) {
          e.printStackTrace();
        } 
        
        try {
        	
          System.out.println("pdfbyte dentro do try" + pdfBytes);
          EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
          
          DynamicVO dynamicVO1 = (DynamicVO)dwfFacade.getDefaultValueObjectInstance("AnexoMensagem");
          
          dynamicVO1.setProperty("ANEXO", pdfBytes);
          dynamicVO1.setProperty("NOMEARQUIVO", "Nota_Debito.pdf");
          dynamicVO1.setProperty("TIPO", "application/pdf");
          
          PersistentLocalEntity createEntity = dwfFacade.createEntity("AnexoMensagem", (EntityVO)dynamicVO1);
          DynamicVO save = (DynamicVO)createEntity.getValueObject();
          
          this.nuAnexo = save.asBigDecimal("NUANEXO");
          System.out.println("NUANEXO " + this.nuAnexo);
          System.out.println("inserido na tela de anexo");
          
        } catch (Exception e) {
          e.printStackTrace();
          this.msg = "Erro na inclusao do anexo " + e.getMessage();
          System.out.println(this.msg);
        } 
        
        try {
        	
          System.out.println("pdfbyte dentro do try" + pdfBytes);
          
          EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
          DynamicVO dynamicVO1 = (DynamicVO)dwfFacade.getDefaultValueObjectInstance("AnexoPorMensagem");
          
          dynamicVO1.setProperty("NUANEXO", this.nuAnexo);
          dynamicVO1.setProperty("CODFILA", this.codFila);
          
          PersistentLocalEntity createEntity = dwfFacade.createEntity("AnexoPorMensagem", (EntityVO)dynamicVO1);
          DynamicVO save = (DynamicVO)createEntity.getValueObject();
          
          System.out.println("NUANEXO " + this.nuAnexo);
          System.out.println("inserido na tela de anexo");
          
        } catch (Exception e) {
          e.printStackTrace();
          this.msg = "Erro na inclusao do anexo " + e.getMessage();
          System.out.println(this.msg);
        } 
        
        this.query2 = "  SELECT DET.ANEXO,"
        		+ " DET.RDV, "
        		+ " DET.VALOR,"
        		+ " FAT.CODPROJ, "
        		+ " FAT.DTVENC  "
        		+ " FROM AD_FATND FAT "
        		+ " FULL JOIN AD_RDVFATND RDV ON FAT.ID = RDV.ID "
        		+ " FULL JOIN AD_DETRDVFAT DET ON DET.ID = FAT.ID "
        		+ " WHERE  DET.ANEXO IS NOT NULL  "
        		+ "AND FAT.CODPROJ = " + codProj  
        		+ " AND FAT.CODPARC = " + codParc;
        ResultSet rs2 = nativeSql.executeQuery(this.query2);
        System.out.println(this.query2);
        while (rs2.next()) {
          try {
        	  
            codProj = rs2.getBigDecimal("CODPROJ");
            this.anexo = rs2.getBytes("ANEXO");
            rdv = rs2.getBigDecimal("RDV");
            byte[] bytes = this.anexo;
            SessionFile fileReport = SessionFile.createSessionFile("Nota_" + this.anexo, "application/pdf", bytes);
            String chaveSessaoArquivo = UIDGenerator.getNextID();
            ServiceContext.getCurrent().putHttpSessionAttribute(chaveSessaoArquivo, (Serializable)fileReport);
            
          } catch (Exception e) {
            e.printStackTrace();
            this.msg = "Erro na inclusao do anexo " + e.getMessage();
            System.out.println(this.msg);
          } 
          
          try {
        	  
            System.out.println("pdfbyte dentro do try" + this.anexo);
            EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
            DynamicVO dynamicVO1 = (DynamicVO)dwfFacade.getDefaultValueObjectInstance("AnexoMensagem");
            dynamicVO1.setProperty("ANEXO", this.anexo);
            dynamicVO1.setProperty("NOMEARQUIVO", "Detalhe Reembolso.pdf");
            dynamicVO1.setProperty("TIPO", "application/pdf");
            PersistentLocalEntity createEntity = dwfFacade.createEntity("AnexoMensagem", (EntityVO)dynamicVO1);
            DynamicVO save = (DynamicVO)createEntity.getValueObject();
            this.nuAnexo1 = save.asBigDecimal("NUANEXO");
            System.out.println("NUANEXO " + this.nuAnexo);
            System.out.println("inserido na tela de anexo");
            
          } catch (Exception e) {
            e.printStackTrace();
            this.msg = "Erro na inclusao do anexo " + e.getMessage();
            System.out.println(this.msg);
          } 
          
          try {
        	  
            System.out.println("pdfbyte dentro do try" + pdfBytes);
            EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
            
            DynamicVO dynamicVO1 = (DynamicVO)dwfFacade.getDefaultValueObjectInstance("AnexoPorMensagem");
            
            dynamicVO1.setProperty("NUANEXO", this.nuAnexo1);
            dynamicVO1.setProperty("CODFILA", this.codFila);
            PersistentLocalEntity createEntity = dwfFacade.createEntity("AnexoPorMensagem", (EntityVO)dynamicVO1);
            DynamicVO save = (DynamicVO)createEntity.getValueObject();
            System.out.println("NUANEXO " + this.nuAnexo);
            System.out.println("inserido na tela de anexo");
            
          } catch (Exception e) {
            e.printStackTrace();
            this.msg = "Erro na inclusao do anexo " + e.getMessage();
            System.out.println(this.msg);
          } 
          
          try {
        	  
            System.out.println("CODFILA na inserdo item" + this.codFila);
            System.out.println("alterado cod");
            
            EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
            
            DynamicVO dynamicVO1 = (DynamicVO)dwfFacade.getDefaultValueObjectInstance("MSDDestFilaMensagem");
            
            dynamicVO1.setProperty("CODFILA", this.codFila);
            dynamicVO1.setProperty("EMAIL", "tassio.vasconcelos@covenantit.com.br");
            dynamicVO1.setProperty("SEQUENCIA", new BigDecimal(1));
            PersistentLocalEntity createEntity = dwfFacade.createEntity("MSDDestFilaMensagem", (EntityVO)dynamicVO1);
            DynamicVO save = (DynamicVO)createEntity.getValueObject();
            System.out.println("CODFILA " + this.codFila);
            
          } catch (Exception e) {
            e.printStackTrace();
            this.msg = "Erro na inclusao do item " + e.getMessage();
            System.out.println(this.msg);
          } 
          
        } 
      } 
    } catch (Exception e) {
      e.printStackTrace();
      this.msg = "Erro no insert do log " + e.getMessage();
      e.printStackTrace();
    } 
    JdbcWrapper.closeSession(JDBC);
    JapeSession.close(hnd);
  }
}
