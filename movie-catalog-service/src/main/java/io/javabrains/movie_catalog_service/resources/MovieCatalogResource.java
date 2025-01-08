package io.javabrains.movie_catalog_service.resources;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import io.javabrains.movie_catalog_service.models.CatalogItem;
import io.javabrains.movie_catalog_service.models.Movie;
import io.javabrains.movie_catalog_service.models.Rating;
import io.javabrains.movie_catalog_service.models.UserRating;

@RestController
@RequestMapping("/catalog")
public class MovieCatalogResource {
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private WebClient.Builder webClientBuilder;
	
	@RequestMapping("/{userId}")
	@HystrixCommand(fallbackMethod = "getFallbackCatalog")
	public List<CatalogItem> getCatalog(@PathVariable("userId") String userId) {
		
		//RestTemplate restTemplate = new RestTemplate();
		
		UserRating userRating  = restTemplate.getForObject("http://ratings-data-service/ratingsdata/user/"+userId, UserRating.class);
		
		return userRating.getRatings().stream()
				.map(rating -> {
					//For each movie ID, call movie info service and get details
					Movie movie = restTemplate.getForObject("http://movie-info-service/movies/" + rating.getMovieId(), Movie.class);
		
			/*
			Movie movie = webClientBuilder.build()
				.get()
				.uri("http://localhost:8082/movies/" + rating.getMovieId())
				.retrieve()
				.bodyToMono(Movie.class)
				.block();
			*/
			//Put them all together
			return new CatalogItem(movie.getName(), movie.getDescription(), rating.getRating());
		})
			.collect(Collectors.toList());
	}
	
	public List<CatalogItem> getFallbackCatalog(@PathVariable("userId") String userId){
		return Arrays.asList(new CatalogItem("No movie", "", 0));
	}
}
