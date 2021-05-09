package it.polito.tdp.extflightdelays.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graphs;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import it.polito.tdp.extflightdelays.db.ExtFlightDelaysDAO;

public class Model {

	private SimpleWeightedGraph<Airport,DefaultWeightedEdge> grafo;
	private ExtFlightDelaysDAO dao;
	private Map<Integer,Airport> idMap;
	private Map<Airport,Airport> visita;
	
	public Model() {
		dao=new ExtFlightDelaysDAO();
		idMap=new HashMap<Integer,Airport>();
		dao.loadAllAirports(idMap);  //riempo la mappa
	}
	
	public void creaGrafo(int x) {
		//lo ricreo da zero ogni volta
		grafo=new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		
		//aggiungo vertici filtrati
		Graphs.addAllVertices(grafo, dao.getVertici(x, idMap));
		
		//aggiungo gli archi
		for(Rotta r:dao.getRotte(idMap)) {
			if(this.grafo.containsVertex(r.getA1()) && this.grafo.containsVertex(r.getA2())) {
				//ho controllato che gli aeroporti della rotta facciano parte del grafo
				//ora controllo se c'è già un arco tra i due aeroporti, se c'è già vuol dire che sto controllando la rotta inversa
				DefaultWeightedEdge e=this.grafo.getEdge(r.getA1(), r.getA2());  //ritorna l'arco a prescindere dall'ordine
				if(e==null) { // se è null vuol dire che non c'è l'arco nè da a1 a a2 nè viceversa
					Graphs.addEdgeWithVertices(grafo, r.getA1(), r.getA2(), r.getN());
				}
				else {
					double pesoVecchio = this.grafo.getEdgeWeight(e);
					double pesoNuovo=pesoVecchio+r.getN();
					this.grafo.setEdgeWeight(e, pesoNuovo);
				}
			}
		}
		System.out.println("num vertici" + grafo.vertexSet().size()+ " num archi "+grafo.edgeSet().size());
		
	}

	public Set<Airport> getVertici() {
		return this.grafo.vertexSet();
	}
	
	public List<Airport> trovaPercorso(Airport a1, Airport a2){
		List<Airport> percorso=new LinkedList<>();  //se ha size zero vuol dire che i percorsi non sono collegati
		
		BreadthFirstIterator<Airport,DefaultWeightedEdge> it=new BreadthFirstIterator<>(grafo,a1);
		
		visita=new HashMap<>();
		visita.put(a1, null);
		//associo all'iteratore un traversal list, una classe che ci permette di reagire a degli eventi
		it.addTraversalListener(new TraversalListener<Airport,DefaultWeightedEdge>(){

			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
			}

			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
			}

			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultWeightedEdge> e) {
				//quando attraversiamo un arco ci salviamo la sorgente dell'arco attraversato, per avere da parte l'alberp di visita	
				Airport air1=grafo.getEdgeSource(e.getEdge());
				Airport air2=grafo.getEdgeTarget(e.getEdge());
				//non è orientato, non sappiamo in realtà quale sia la sorgente e la destinazione
				if(visita.containsKey(air1) && !visita.containsKey(air2)) {
					//vuol dire che a2 viene scoperto da a1
					visita.put(air2, air1);
				}
				else if(visita.containsKey(air2) && !visita.containsKey(air1)){
					visita.put(air1, air2);
				}
			}

			@Override
			public void vertexTraversed(VertexTraversalEvent<Airport> e) {
			}

			@Override
			public void vertexFinished(VertexTraversalEvent<Airport> e) {
			}
			
		});
	
		while(it.hasNext()) {
			it.next();
		}
		
		percorso.add(a2);
		Airport step=a2;
		while(visita.get(step)!=null) {
			step=visita.get(step);
			percorso.add(step);
		}
		
		return percorso;
	}
	
	public int getNVertici() {
		if(grafo != null)
			return grafo.vertexSet().size();
		
		return 0;
	}
	
	public int getNArchi() {
		if(grafo != null)
			return grafo.edgeSet().size();
		
		return 0;
	}


}
