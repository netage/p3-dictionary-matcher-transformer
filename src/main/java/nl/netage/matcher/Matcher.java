package nl.netage.matcher;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.SyncTransformer;
import eu.fusepool.p3.transformer.TransformerException;
import eu.fusepool.p3.transformer.commons.Entity;
import eu.fusepool.p3.transformer.dictionarymatcher.DictionaryMatcherTransformer;

/**
 * Servlet implementation class Matcher
 */
public class Matcher extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Matcher() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		SyncTransformer transformer = new DictionaryMatcherTransformer();
		try {
		Entity responseEntity = transformer.transform(new HttpRequestEntity(request));
		writeResponse(responseEntity, response);
	} catch (TransformerException e) {
		e.printStackTrace();
		response.setStatus(e.getStatusCode());
		writeResponse(e.getResponseEntity(), response);
	}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		SyncTransformer transformer = new DictionaryMatcherTransformer(request.getQueryString());
		try {
			Entity responseEntity = transformer.transform(new HttpRequestEntity(request));
			writeResponse(responseEntity, response);
		} catch (TransformerException e) {
			e.printStackTrace();
			response.setStatus(e.getStatusCode());
			writeResponse(e.getResponseEntity(), response);
		}
	}

	static void writeResponse(Entity responseEntity, HttpServletResponse response) throws IOException {
		response.setContentType(responseEntity.getType().toString());
		responseEntity.writeData(response.getOutputStream());
		response.getOutputStream().flush();
	}

}
