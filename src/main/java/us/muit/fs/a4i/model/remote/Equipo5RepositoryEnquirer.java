/**
 * 
 */
package us.muit.fs.a4i.model.remote;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRepositoryStatistics;
import org.kohsuke.github.GHRepositoryStatistics.CodeFrequency;
import org.kohsuke.github.GitHub;

import us.muit.fs.a4i.exceptions.MetricException;
import us.muit.fs.a4i.exceptions.ReportItemException;
import us.muit.fs.a4i.model.entities.Report;
import us.muit.fs.a4i.model.entities.ReportI;
import us.muit.fs.a4i.model.entities.ReportItem;
import us.muit.fs.a4i.model.entities.ReportItem.ReportItemBuilder;

/**
 * @author Isabel Román
 *
 */
public class Equipo5RepositoryEnquirer extends GitHubEnquirer {
	private static Logger log = Logger.getLogger(Equipo5RepositoryEnquirer.class.getName());

	/**
	 * <p>
	 * Constructor
	 * </p>
	 */

	public Equipo5RepositoryEnquirer() {
		super();
		metricNames.add("pullRequestsAcceptedLastYear");
		metricNames.add("pullRequestsAcceptedLastMonth");
		metricNames.add("pullRequestsRejectedLastYear");
		metricNames.add("pullRequestsRejectedLastMonth");
		log.info("Añadidas métricas al E5RepositoryEnquirer");
	}

	@Override
	public ReportI buildReport(String repositoryId) {
		ReportI myRepo = null;
		log.info("Invocado el m�todo que construye un objeto RepositoryReport");
		/**
		 * <p>
		 * Información sobre el repositorio obtenida de GitHub
		 * </p>
		 */
		GHRepository remoteRepo;
		/**
		 * <p>
		 * En estos momentos cada vez que se invoca construyeObjeto se crea y rellena
		 * uno nuevo
		 * </p>
		 * <p>
		 * Deuda técnica: se puede optimizar consultando sólo las diferencias respecto a
		 * la fecha de la última representación local
		 * </p>
		 */

		try {
			log.info("Nombre repo = " + repositoryId);

			GitHub gb = getConnection();
			remoteRepo = gb.getRepository(repositoryId);
			log.info("El repositorio es de " + remoteRepo.getOwnerName() + " Y su descripción es "
					+ remoteRepo.getDescription());
			log.info("leído " + remoteRepo);
			myRepo = new Report(repositoryId);

			/**
			 * Métricas propias definidas
			 */
			
			List<GHPullRequest> pullRequests = remoteRepo.getPullRequests(GHIssueState.CLOSED);
			
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneMonthAgo = now.minusMonths(1);
            LocalDateTime oneYearAgo = now.minusYears(1);
            
            List<GHPullRequest> acceptedLastYear = filterPullRequests(pullRequests, oneYearAgo, now, true);
            List<GHPullRequest> acceptedLastMonth = filterPullRequests(pullRequests, oneMonthAgo, now, true);
            List<GHPullRequest> rejectedLastYear = filterPullRequests(pullRequests, oneYearAgo, now, false);
            List<GHPullRequest> rejectedLastMonth = filterPullRequests(pullRequests, oneMonthAgo, now, false);
		
			// pullRequestsAcceptedLastYear
			ReportItemBuilder<Double> pullRequestsAcceptedLastYear = new ReportItem.ReportItemBuilder<Double>("pullRequestsAcceptedLastYear",
					(double)acceptedLastYear.size());
			pullRequestsAcceptedLastYear.source("GitHub");
			myRepo.addMetric(pullRequestsAcceptedLastYear.build());
			log.info("Añadida métrica pullRequestsAcceptedLastYear " + pullRequestsAcceptedLastYear);
			
			// pullRequestsAcceptedLastMonth
			ReportItemBuilder<Double> pullRequestsAcceptedLastMonth = new ReportItem.ReportItemBuilder<Double>("pullRequestsAcceptedLastMonth",
					(double)acceptedLastMonth.size());
			pullRequestsAcceptedLastMonth.source("GitHub");
			myRepo.addMetric(pullRequestsAcceptedLastMonth.build());
			log.info("Añadida métrica pullRequestsAcceptedLastMonth " + pullRequestsAcceptedLastMonth);
			
			// pullRequestsRejectedLastMonth
			ReportItemBuilder<Double> pullRequestsRejectedLastMonth = new ReportItem.ReportItemBuilder<Double>("pullRequestsRejectedLastMonth",
					(double)rejectedLastMonth.size());
			pullRequestsRejectedLastMonth.source("GitHub");
			myRepo.addMetric(pullRequestsRejectedLastMonth.build());
			log.info("Añadida métrica pullRequestsRejectedLastMonth " + pullRequestsRejectedLastMonth);

			// pullRequestsRejectedLastYear
			ReportItemBuilder<Double> pullRequestsRejectedLastYear = new ReportItem.ReportItemBuilder<Double>("pullRequestsRejectedLastYear",
					(double)rejectedLastYear.size());
			pullRequestsRejectedLastYear.source("GitHub");
			myRepo.addMetric(pullRequestsRejectedLastYear.build());
			log.info("Añadida métrica pullRequestsRejectedLastYear " + pullRequestsRejectedLastYear);


		} catch (Exception e) {
			log.severe("Problemas en la conexión " + e);
		}

		return myRepo;
	}

	/**
	 * Permite consultar desde fuera una métrica del repositorio indicado
	 */

	@Override
	public ReportItem getMetric(String metricName, String repositoryId) throws MetricException {
		GHRepository remoteRepo;

		GitHub gb = getConnection();
		try {
			remoteRepo = gb.getRepository(repositoryId);
		} catch (Exception e) {
			e.printStackTrace();
			throw new MetricException(
					"No se puede acceder al repositorio remoto " + repositoryId + " para recuperarlo");
		}

		return getMetric(metricName, remoteRepo);
	}

	/**
	 * <p>
	 * Crea la métrica solicitada consultando el repositorio remoto que se pasa como
	 * parámetro
	 * </p>
	 * 
	 * @param metricName Métrica solicitada
	 * @param remoteRepo Repositorio remoto
	 * @return La métrica creada
	 * @throws MetricException Si la métrica no está definida se lanzará una
	 *                         excepción
	 */
	private ReportItem getMetric(String metricName, GHRepository remoteRepo) throws MetricException {
		ReportItem metric;
		if (remoteRepo == null) {
			throw new MetricException("Intenta obtener una métrica sin haber obtenido los datos del repositorio");
		}
		switch (metricName) {
		
		case "pullRequestsAcceptedLastYear":
			metric = pullRequestsAcceptedLastYear(remoteRepo);
			break;
		case "pullRequestsAcceptedLastMonth":
			metric = pullRequestsAcceptedLastMonth(remoteRepo);
			break;
		case "pullRequestsRejectedLastYear":
			metric = pullRequestsRejectedLastYear(remoteRepo);
			break;
		case "pullRequestsRejectedLastMonth":
			metric = pullRequestsRejectedLastMonth(remoteRepo);
			break;
		default:
			throw new MetricException("La métrica " + metricName + " no está definida para un repositorio");
		}
		

		return metric;
	}

	/*
	 * A partir de aquí los algoritmos específicos para hacer las consultas de cada
	 * métrica
	 */

	/**
	 * <p>
	 * Obtención del número total de adiciones al repositorio
	 * </p>
	 * 
	 * @param remoteRepo el repositorio remoto sobre el que consultar
	 * @return la métrica con el número total de adiciones desde el inicio
	 * @throws MetricException Intenta crear una métrica no definida
	 */
	private ReportItem getTotalAdditions(GHRepository remoteRepo) throws MetricException {
		ReportItem metric = null;

		GHRepositoryStatistics data = remoteRepo.getStatistics();
		List<CodeFrequency> codeFreq;
		try {
			codeFreq = data.getCodeFrequency();

			int additions = 0;

			for (CodeFrequency freq : codeFreq) {

				if (freq.getAdditions() != 0) {
					Date fecha = new Date((long) freq.getWeekTimestamp() * 1000);
					log.info("Fecha modificaciones " + fecha);
					additions += freq.getAdditions();

				}
			}
			ReportItemBuilder<Integer> totalAdditions = new ReportItem.ReportItemBuilder<Integer>("totalAdditions",
					additions);
			totalAdditions.source("GitHub, calculada")
					.description("Suma el total de adiciones desde que el repositorio se creó");
			metric = totalAdditions.build();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ReportItemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return metric;

	}

	/**
	 * <p>
	 * Obtención del número total de eliminaciones del repositorio
	 * </p>
	 * 
	 * @param remoteRepo el repositorio remoto sobre el que consultar
	 * @return la métrica con el n�mero total de eliminaciones desde el inicio
	 * @throws MetricException Intenta crear una métrica no definida
	 */
	private ReportItem getTotalDeletions(GHRepository remoteRepo) throws MetricException {
		ReportItem metric = null;

		GHRepositoryStatistics data = remoteRepo.getStatistics();
		List<CodeFrequency> codeFreq;
		try {
			codeFreq = data.getCodeFrequency();

			int deletions = 0;

			for (CodeFrequency freq : codeFreq) {

				if (freq.getDeletions() != 0) {
					Date fecha = new Date((long) freq.getWeekTimestamp() * 1000);
					log.info("Fecha modificaciones " + fecha);
					deletions += freq.getAdditions();

				}
			}
			ReportItemBuilder<Integer> totalDeletions = new ReportItem.ReportItemBuilder<Integer>("totalDeletions",
					deletions);
			totalDeletions.source("GitHub, calculada")
					.description("Suma el total de eliminaciones desde que el repositorio se cre�");
			metric = totalDeletions.build();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ReportItemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return metric;

	}


    /**
     * <p>
     * Filtra las solicitudes de extracción (pull requests) según la fecha de creación y el estado de aceptación.
     * </p>
     *
     * @param pullRequests la lista de solicitudes de extracción a filtrar
     * @param startDate la fecha de inicio del intervalo de tiempo para filtrar
     * @param endDate la fecha de finalización del intervalo de tiempo para filtrar
     * @param accepted indica si se deben filtrar las solicitudes de extracción aceptadas (true) o rechazadas (false)
     * @return la lista de solicitudes de extracción que cumplen con los criterios de filtrado
     */
    private static List<GHPullRequest> filterPullRequests(List<GHPullRequest> pullRequests, LocalDateTime startDate, LocalDateTime endDate, boolean accepted) {
        return pullRequests.stream()
                .filter(pr -> {                
                	try {
	                    LocalDateTime createdDate = pr.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	                    return createdDate.isAfter(startDate) && createdDate.isBefore(endDate) && (accepted ? pr.isMerged() : !pr.isMerged());
	                } catch (IOException e) {
	                    log.warning("Failed to get creation date for PR #" + pr.getNumber() + "\n"+e);
	                    return false;
	                }
                })
                .collect(Collectors.toList());
    }
    

    /**
     * <p>
     * Obtención del número de solicitudes de extracción aceptadas en el último año.
     * </p>
     * 
     * @param remoteRepo el repositorio remoto sobre el que consultar
     * @return la métrica con el número de solicitudes de extracción aceptadas en el último año
     * @throws MetricException si ocurre un error al obtener las solicitudes de extracción
     */
    private ReportItem pullRequestsAcceptedLastYear(GHRepository remoteRepo) throws MetricException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusYears(1);
        
        try {
            List<GHPullRequest> pullRequests = remoteRepo.getPullRequests(GHIssueState.CLOSED);
            List<GHPullRequest> acceptedLastYear = filterPullRequests(pullRequests, oneYearAgo, now, true);
            
            ReportItemBuilder<Integer> acceptedLastYearMetric = new ReportItem.ReportItemBuilder<>("pullRequestsAcceptedLastYear", acceptedLastYear.size());
            acceptedLastYearMetric.source("GitHub, calculada")
                    .description("Número de solicitudes de extracción aceptadas en el último año");
            
            return acceptedLastYearMetric.build();
        } catch (IOException | ReportItemException e) {
            throw new MetricException("Error al obtener las solicitudes de extracción aceptadas en el último año\n" + e);
        }
    }
    
    /**
     * <p>
     * Obtención del número de solicitudes de extracción aceptadas en el último mes.
     * </p>
     * 
     * @param remoteRepo el repositorio remoto sobre el que consultar
     * @return la métrica con el número de solicitudes de extracción aceptadas en el último mes
     * @throws MetricException si ocurre un error al obtener las solicitudes de extracción
     */
    private ReportItem pullRequestsAcceptedLastMonth(GHRepository remoteRepo) throws MetricException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        
        try {
            List<GHPullRequest> pullRequests = remoteRepo.getPullRequests(GHIssueState.CLOSED);
            List<GHPullRequest> acceptedLastMonth = filterPullRequests(pullRequests, oneMonthAgo, now, true);
            
            ReportItemBuilder<Integer> acceptedLastMonthMetric = new ReportItem.ReportItemBuilder<>("pullRequestsAcceptedLastMonth", acceptedLastMonth.size());
            acceptedLastMonthMetric.source("GitHub, calculada")
                    .description("Número de solicitudes de extracción aceptadas en el último mes");
            
            return acceptedLastMonthMetric.build();
        } catch (IOException | ReportItemException e) {
            throw new MetricException("Error al obtener las solicitudes de extracción aceptadas en el último mes\n" + e);
        }
    }
    
    
    /**
     * <p>
     * Obtención del número de solicitudes de extracción rechazadas en el último año.
     * </p>
     * 
     * @param remoteRepo el repositorio remoto sobre el que consultar
     * @return la métrica con el número de solicitudes de extracción rechazadas en el último año
     * @throws MetricException si ocurre un error al obtener las solicitudes de extracción
     */
    private ReportItem pullRequestsRejectedLastYear(GHRepository remoteRepo) throws MetricException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusYears(1);
        
        try {
            List<GHPullRequest> pullRequests = remoteRepo.getPullRequests(GHIssueState.CLOSED);
            List<GHPullRequest> rejectedLastYear = filterPullRequests(pullRequests, oneYearAgo, now, false);
            
            ReportItemBuilder<Integer> rejectedLastYearMetric = new ReportItem.ReportItemBuilder<>("pullRequestsRejectedLastYear", rejectedLastYear.size());
            rejectedLastYearMetric.source("GitHub, calculada")
                    .description("Número de solicitudes de extracción rechazadas en el último año");
            
            return rejectedLastYearMetric.build();
        } catch (IOException | ReportItemException e) {
            throw new MetricException("Error al obtener las solicitudes de extracción rechazadas en el último año\n" + e);
        }
    }
    

    /**
     * <p>
     * Obtención del número de solicitudes de extracción rechazadas en el último mes.
     * </p>
     * 
     * @param remoteRepo el repositorio remoto sobre el que consultar
     * @return la métrica con el número de solicitudes de extracción rechazadas en el último mes
     * @throws MetricException si ocurre un error al obtener las solicitudes de extracción
     */
    private ReportItem pullRequestsRejectedLastMonth(GHRepository remoteRepo) throws MetricException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        
        try {
            List<GHPullRequest> pullRequests = remoteRepo.getPullRequests(GHIssueState.CLOSED);
            List<GHPullRequest> rejectedLastMonth = filterPullRequests(pullRequests, oneMonthAgo, now, false);
            
            ReportItemBuilder<Integer> rejectedLastMonthMetric = new ReportItem.ReportItemBuilder<>("pullRequestsRejectedLastMonth", rejectedLastMonth.size());
            rejectedLastMonthMetric.source("GitHub, calculada")
                    .description("Número de solicitudes de extracción rechazadas en el último mes");
            
            return rejectedLastMonthMetric.build();
        } catch (IOException | ReportItemException e) {
            throw new MetricException("Error al obtener las solicitudes de extracción rechazadas en el último mes\n" + e);
        }
    }
}
