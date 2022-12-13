package br.com.crowe.sendEmail;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.AgendamentoRelatorioHelper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.MaskFormatter;

public class EnviarNDEmailAction implements AcaoRotinaJava {
	private static final SimpleDateFormat ddMMyyy = new SimpleDateFormat("dd/MM/yyyy");
	private static final BigDecimal NURFE_NOTA_DEBITO = new BigDecimal(26);

	String coduso;
	String email;
	String emailCtt;
	
	
	BigDecimal codParc;
	BigDecimal vlr;
	Timestamp dtVenc;
	BigDecimal id;
	BigDecimal codProj;
	BigDecimal cnpj;
	BigDecimal agencia;
	BigDecimal codCtaBco;
	BigDecimal bco;
	
	String nomeParc;
	String razaoSocial;
	String abreviatura;
	String nomeBanco;

	BigDecimal codFila;

	public void doAction(ContextoAcao contexto) throws Exception {
		JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(jdbc);
		
		System.out.println("INICIO DO CODIGO");

		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbc = dwfEntityFacade.getJdbcWrapper();

			byte b;
			int i;
			Registro[] arrayOfRegistro;
			for (i = (arrayOfRegistro = contexto.getLinhas()).length, b = 0; b < i;) {

				Registro linha = arrayOfRegistro[b];
				ConcatenatePDF concatenate = new ConcatenatePDF();
				BigDecimal id = (BigDecimal) linha.getCampo("ID");
				BigDecimal codParc = (BigDecimal) linha.getCampo("CODPARC");
				NativeSql sqlEmail = new NativeSql(jdbc);

				sqlEmail.appendSql(" SELECT EMAIL ");
				sqlEmail.appendSql(" FROM TGFPAR ");
				sqlEmail.appendSql(" WHERE CODPARC = ?");
				sqlEmail.addParameter(codParc);

				ResultSet rsEmail = sqlEmail.executeQuery();
				String emailParc = null;

			/*	if (rsEmail.next())
					emailParc = rsEmail.getString("EMAIL");
				rsEmail.close();
				if (emailParc == null) {
					contexto.setMensagemRetorno("Parceiro sem email! \n Favor Cadastrar Email.");
					return;
				} */

				AgendamentoRelatorioHelper.ParametroRelatorio pk = new AgendamentoRelatorioHelper.ParametroRelatorio(
						"PK_ID", BigDecimal.class.getName(), id);
				List<Object> lstParam = new ArrayList();
				lstParam.add(pk);
				byte[] pdfNDBytes = AgendamentoRelatorioHelper.getPrintableReport(NURFE_NOTA_DEBITO, lstParam,
						contexto.getUsuarioLogado(), dwfEntityFacade);
				concatenate.addPdfFile(pdfNDBytes);
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
				byte[] pdfBytes = bytesPdf.toByteArray();
				
				String queryId = " SELECT FAT.CODPARC, cast((FAT.VALOR) as numeric(20,2)) AS VLR, FAT.DTVENC, FAT.ID, FAT.CODPROJ, "
						+ "	PAR.NOMEPARC, EMP.RAZAOSOCIAL, PRJ.ABREVIATURA, EMP.CODEMP, EMP.NOMEFANTASIA, EMP.CGC,"
						+ "	CTA.CODAGE, EMP.AD_CODBCO, BCO.CODBCO, BCO.NOMEBCO, CTA.CODCTABCO"
						+ "	FROM AD_FATND FAT "
						+ "	JOIN TGFPAR PAR ON PAR.CODPARC = FAT.CODPARC"
						+ "	JOIN TSIEMP EMP ON EMP.CODEMP = FAT.CODEMP"
						+ "	JOIN TCSPRJ PRJ ON PRJ.CODPROJ = FAT.CODPROJ"
						+ "	INNER JOIN TSICTA CTA ON ( CTA.CODCTABCOINT = AD_CODCTABCOINT2)"
						+ "	RIGHT JOIN TSIBCO BCO ON BCO.CODBCO = EMP.AD_CODBCO"
						+ "	WHERE ID = " +id;
				ResultSet rsId = nativeSql.executeQuery(queryId);
				
				while (rsId.next()) {
					codParc = rsId.getBigDecimal("CODPARC");
					vlr = rsId.getBigDecimal("VLR");
					dtVenc = rsId.getTimestamp("DTVENC");
					id = rsId.getBigDecimal("ID");
					codProj = rsId.getBigDecimal("CODPROJ");
					nomeParc = rsId.getString("NOMEPARC");
					razaoSocial = rsId.getString("RAZAOSOCIAL");
					abreviatura = rsId.getString("ABREVIATURA");
					cnpj = rsId.getBigDecimal("CGC");
					agencia = rsId.getBigDecimal("CODAGE");
					codCtaBco = rsId.getBigDecimal("CODCTABCO");
					bco = rsId.getBigDecimal("CODBCO");
					nomeBanco = rsId.getString("NOMEBCO");
				}
				
				MaskFormatter mask = new MaskFormatter("##.###.###/####-##");
				mask.setValueContainsLiteralCharacters(false);
				System.out.println("CNPJ : " + mask.valueToString(cnpj));

				String assuntoEmail = "Prezado(a), " +nomeParc 
						+ "\r\n"
						+ "\r\n"
						+ "Segue anexo NOTA DE DEBITO "+id+", com  vencimento em " +ddMMyyy.format(dtVenc)+", referente as despesas incorridas no projeto "+abreviatura+".\r\n"
						+ "\r\n"
						+ "Dados bancários para pagamento:\r\n"
						+ "\r\n"
						+ razaoSocial+"\r\n"
						+ "CNPJ (PIX): "+ mask.valueToString(cnpj)+"\r\n"
						+ "Banco: "+bco+ " - " +nomeBanco+"\r\n"
						+ "Ag "+agencia+"\r\n"
						+ "Conta Corrente: "+codCtaBco+"\r\n"
						+ "\r\n"
						+ "Ficamos disposição para esclarecer quaisquer dúvidas.\r\n"
						+ "";

				char[] assuntoEmailchar = assuntoEmail.toCharArray();

				String queryEmail = ("SELECT MAX(EMAIL) as EMAIL FROM TGFCTT WHERE CODPARC = " + codParc
						+ " AND NOMECONTATO <> 'COBRANCA'");

				ResultSet rs = nativeSql.executeQuery(queryEmail);
				System.out.println(rs);
				while (rs.next()) {
					email = rs.getString("EMAIL");
					System.out.println("Dentro do while : " + email);
				}

				System.out.println("email" + email);

				try {

					DynamicVO filaMensagemVO = (DynamicVO) dwfEntityFacade
							.getDefaultValueObjectInstance("MSDFilaMensagem");
					filaMensagemVO.setProperty("ASSUNTO", "NOTA DE DEBITO "+id+" - vencimento "+ddMMyyy.format(dtVenc)+" -"+razaoSocial);
					filaMensagemVO.setProperty("CODMSG", null);
					filaMensagemVO.setProperty("DTENTRADA", new Timestamp(System.currentTimeMillis()));
					filaMensagemVO.setProperty("STATUS", "Pendente");
					filaMensagemVO.setProperty("CODCON", new BigDecimal(0));
					filaMensagemVO.setProperty("TENTENVIO", new BigDecimal(0));
					filaMensagemVO.setProperty("MENSAGEM", assuntoEmailchar);
					filaMensagemVO.setProperty("TIPOENVIO", "E");
					filaMensagemVO.setProperty("MAXTENTENVIO", new BigDecimal(3));
					//filaMensagemVO.setProperty("EMAIL", "t.santos.vasconcelos@gmail.com");
				    filaMensagemVO.setProperty("EMAIL", email);
					filaMensagemVO.setProperty("CODSMTP", new BigDecimal(2));
					filaMensagemVO.setProperty("CODUSUREMET", this.coduso);

					PersistentLocalEntity createFilaMensagem = dwfEntityFacade.createEntity("MSDFilaMensagem",
							(EntityVO) filaMensagemVO);
					filaMensagemVO = (DynamicVO) createFilaMensagem.getValueObject();
					codFila = filaMensagemVO.asBigDecimal("CODFILA");

				} catch (Exception e) {
					e.printStackTrace();
				}

				DynamicVO anexoVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoMensagem");

				anexoVO.setProperty("ANEXO", pdfBytes);
				anexoVO.setProperty("NOMEARQUIVO", "Nota_Debito.pdf");
				anexoVO.setProperty("TIPO", "application/pdf");

				PersistentLocalEntity createAnexo = dwfEntityFacade.createEntity("AnexoMensagem", (EntityVO) anexoVO);
				anexoVO = (DynamicVO) createAnexo.getValueObject();

				BigDecimal nuAnexo = anexoVO.asBigDecimal("NUANEXO");
				DynamicVO anexoMensagemVO = (DynamicVO) dwfEntityFacade
						.getDefaultValueObjectInstance("AnexoPorMensagem");

				anexoMensagemVO.setProperty("NUANEXO", nuAnexo);
				anexoMensagemVO.setProperty("CODFILA", codFila);

				dwfEntityFacade.createEntity("AnexoPorMensagem", (EntityVO) anexoMensagemVO);

				DynamicVO destFilaMensagemVO = (DynamicVO) dwfEntityFacade
						.getDefaultValueObjectInstance("MSDDestFilaMensagem");

				destFilaMensagemVO.setProperty("CODFILA", codFila);
				destFilaMensagemVO.setProperty("EMAIL", "cobranca@crowe.com.br");
				destFilaMensagemVO.setProperty("SEQUENCIA", new BigDecimal(1));
				dwfEntityFacade.createEntity("MSDDestFilaMensagem", (EntityVO) destFilaMensagemVO);
				bytesPdf.close();
				b++;

				System.out.println("codparc" + codParc);

				System.out.println("email do contato : " + email);

				String queryEmailParc = ("SELECT EMAIL FROM TGFCTT WHERE CODPARC = " + codParc
						+ " AND NOMECONTATO <> 'COBRANCA'" + " AND EMAIL <> '" + email + "'"
						+ " AND RECEBEBOLETOEMAIL = 'S'" + " AND ATIVO = 'S'"
						+ " AND EMAIL <> 'cobranca@crowe.com.br'");
				ResultSet rsCtt = nativeSql.executeQuery(queryEmailParc);

				System.out.println(queryEmailParc);

				while (rsCtt.next()) {

					emailCtt = rsCtt.getString("EMAIL");
					System.out.println("emailCtt do contato : " + emailCtt);
					System.out.println("email do contato : " + email);
					System.out.println("Dentro do while" + queryEmailParc);

					try {

						System.out.println("CODFILA na inserção do item" + codFila);
						System.out.println("alteração do cod");

						EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
						DynamicVO dynamicVO1 = (DynamicVO) dwfFacade
								.getDefaultValueObjectInstance("MSDDestFilaMensagem");

						dynamicVO1.setProperty("CODFILA", codFila);
						dynamicVO1.setProperty("EMAIL", emailCtt);
						// dynamicVO1.setProperty("EMAIL", "jefferson.costa@covenantit.com.br");
						//dynamicVO1.setProperty("EMAIL", "fabio.cavalcante@crowe.com.br");
						dynamicVO1.setProperty("SEQUENCIA", new BigDecimal(2));
						PersistentLocalEntity createEntity = dwfFacade.createEntity("MSDDestFilaMensagem",
								(EntityVO) dynamicVO1);
						DynamicVO save = (DynamicVO) createEntity.getValueObject();

						System.out.println("CODFILA " + codFila);

					} catch (Exception e) {
						e.printStackTrace();
						String msg = "Erro na inclusao do item " + e.getMessage();
						System.out.println(msg);
					}
				}

			}
			boolean update = nativeSql
					.executeUpdate(" UPDATE AD_FATND SET STATUS = 'E' WHERE ID = " + id);
			System.out.println(update);
			
			contexto.setMensagemRetorno("Email enviado com sucesso");
		} finally {
			JdbcWrapper.closeSession(jdbc);
		}
	}
}
