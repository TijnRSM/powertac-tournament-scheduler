package org.powertac.tournament.listeners;

import org.powertac.tournament.services.RestBroker;
import org.powertac.tournament.services.RestServer;
import org.powertac.tournament.services.RestVisualizer;

import javax.faces.FacesException;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;


public class RestoreViewPhaseListener implements PhaseListener
{
  private static final Object BROKER_LOGIN_LOCK = new Object();
  private static final Object SERVER_INTERFACE_LOCK = new Object();

  // Intercepts REST calls
  public void beforePhase (PhaseEvent pe)
  {
    HttpServletRequest request = (HttpServletRequest) pe.getFacesContext().
        getExternalContext().getRequest();
    Map<String, String[]> params = getParams(request);

    if (params.size() != 0) {
      RestServer restServer = new RestServer();
      RestVisualizer restVisualizer = new RestVisualizer();
      RestBroker restBroker = new RestBroker();
      String url = request.getRequestURL().toString();

      if (url.contains("serverInterface.jsp")) {
        synchronized (SERVER_INTERFACE_LOCK) {
          if (request.getMethod().equals("PUT")) {
            respond(pe, restServer.handlePUT(params, request));
          } else if (request.getMethod().equals("POST")) {
            respond(pe, restServer.handlePOST(params, request));
          } else {
            respond(pe, restServer.handleGet(params, request));
          }
        }
      } else if (url.contains("brokerLogin.jsp")) {
        synchronized (BROKER_LOGIN_LOCK) {
          respond(pe, restBroker.parseBrokerLogin(params));
        }
      } else if (url.contains("visualizerLogin.jsp")) {
        respond(pe, restVisualizer.parseVisualizerLogin(params, request));
      } else if (url.contains("properties.jsp")) {
        respond(pe, restServer.parseProperties(params, request));
      } else if (url.contains("pom.jsp")) {
        respond(pe, restServer.parsePom(params));
      }
    }
  }

  private void respond (PhaseEvent pe, String responseString)
  {
    if (responseString.isEmpty()) {
      return;
    }

    HttpServletResponse response = (HttpServletResponse) pe.getFacesContext().
        getExternalContext().getResponse();
    response.setContentType("text/plain; charset=UTF-8");

    try {
      PrintWriter pw = response.getWriter();
      pw.print(responseString);
    } catch (IOException ex) {
      throw new FacesException(ex);
    }

    pe.getFacesContext().responseComplete();
  }

  // Which jsf phase to intercept, in this case the Restore View Phase
  public PhaseId getPhaseId ()
  {
    return PhaseId.RESTORE_VIEW;
  }

  public void afterPhase (PhaseEvent arg0)
  {
  }

  private Map<String, String[]> getParams (HttpServletRequest request)
  {
    @SuppressWarnings("unchecked")
    Map<String, String[]> params = request.getParameterMap();
    return params;
  }
}