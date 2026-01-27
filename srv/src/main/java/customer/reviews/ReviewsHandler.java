package customer.reviews;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Delete;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.sap.capire.reviews.Likes;
import cds.gen.sap.capire.reviews.api.reviewsservice.AverageRatingsChanged;
import cds.gen.sap.capire.reviews.api.reviewsservice.AverageRatingsChangedContext;
import cds.gen.sap.capire.reviews.app.reviewsservice.ReviewsService_;
import cds.gen.sap.capire.reviews.app.reviewsservice.LikeContext;
import cds.gen.sap.capire.reviews.app.reviewsservice.Likes_;
import cds.gen.sap.capire.reviews.app.reviewsservice.Reviews;
import cds.gen.sap.capire.reviews.app.reviewsservice.Reviews_;
import cds.gen.sap.capire.reviews.app.reviewsservice.UnlikeContext;

@Component
@ServiceName(ReviewsService_.CDS_NAME)
public class ReviewsHandler implements EventHandler {

  @Autowired
  @Qualifier(ReviewsService_.CDS_NAME)
  CqnService service;

  @Autowired
  @Qualifier(cds.gen.sap.capire.reviews.api.reviewsservice.ReviewsService_.CDS_NAME)
  CqnService serviceApi;

  private final PersistenceService persistenceService;
  private final Random random;

  public ReviewsHandler(PersistenceService persistenceService) {
    this.persistenceService = persistenceService;
    this.random = new Random(System.currentTimeMillis());
  }

  @On(event = { CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE } )
  public void onUpdateReview(EventContext context, Reviews review) {
    if(review.getRating()==null)
      review.setRating(random.nextInt(4)+1);
  }

  @On(service = ReviewsService_.CDS_NAME, event = "like")
  public void onLikes(LikeContext context) {
    String reviewer = context.getReview().getReviewer();
    String subject = context.getReview().getSubject();
    try {
      service.run(Insert.into(Likes_.CDS_NAME)
        .entry(Map.of(
          "user", context.getUserInfo().getName(), 
          "review_reviewer", reviewer,
          "review", Map.of("subject",subject))));

      Map<String, Object> filter = new HashMap<>();
      filter.put(Reviews.REVIEWER, reviewer);
      filter.put(Reviews.SUBJECT, subject);
      
      CqnUpdate update = Update.entity(cds.gen.sap.capire.reviews.Reviews_.CDS_NAME)
        .set(Reviews.LIKED, CQL.get(Reviews.LIKED).plus(1))
        .matching(filter);

      Result updateResult = persistenceService.run(update);
      if(updateResult.rowCount()!=1)
        throw new ServiceException("Failed to update the liked counter");

    } catch(Exception e) {
      if(e.getMessage().startsWith("Unique constraint violated"))
        throw new ServiceException("You already liked that review", e);
      throw new ServiceException("Failed to perform the operation", e);
    }
    context.setCompleted();
  }

  @On(service = ReviewsService_.CDS_NAME, event = "unlike")
  public void onUnLikes(UnlikeContext context) {
    String reviewer = context.getReview().getReviewer();
    String subject = context.getReview().getSubject();
    try {
      CqnDelete delete = Delete.from(Likes_.CDS_NAME)
        .where(b -> b.get(Likes.USER).eq(CQL.param("user"))
          .and(b.get(Likes.REVIEW_REVIEWER).eq(CQL.param("reviewer")))
          .and(b.get(Likes.REVIEW_SUBJECT).eq(CQL.param("subject"))));

      Map<String, Object> paramValues = new HashMap<>();
      paramValues.put("user", context.getUserInfo().getName());
      paramValues.put("reviewer", reviewer);
      paramValues.put("subject", subject);

      Result deleteResult = service.run(delete, paramValues);
      if(deleteResult.rowCount()!=1)
        throw new ServiceException("Failed to delete the like");

      Map<String, Object> filter = new HashMap<>();
      filter.put(Reviews.REVIEWER, reviewer);
      filter.put(Reviews.SUBJECT, subject);

      CqnUpdate update = Update.entity(cds.gen.sap.capire.reviews.Reviews_.CDS_NAME)
        .set(Reviews.LIKED, CQL.get(Reviews.LIKED).minus(1))
        .matching(filter);

      Result updateResult = persistenceService.run(update);
      if(updateResult.rowCount()!=1)
        throw new ServiceException("Failed to update the liked counter");

    } catch(Exception e) {
      throw new ServiceException("Failed to perform the operation", e);
    }
    context.setCompleted();
  }

  @After(event = { CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE, CqnService.EVENT_DELETE } )
  public void afterChangeReview(EventContext context, Reviews review) {
    Result result = service.run(Select.from(Reviews_.CDS_NAME)
      .columns(r -> CQL.count(r.get("subject")).as("count"), r -> CQL.average(r.get("rating")).as("avg"))
      .where(o -> o.get("subject").eq(review.getSubject())));
    Row selected = result.single();
    String subject = review.getSubject();
    Long count = (Long) selected.get("count");
    BigDecimal avgRating = (BigDecimal) selected.get("avg");
    sendReviewedMessage(subject, count.intValue(), avgRating.intValue());
  }

  private void sendReviewedMessage(String subject, Integer count, Integer avgRating) {
    AverageRatingsChanged event = AverageRatingsChanged.create();
    event.setSubject(subject);
    event.setReviews(count);
    event.setRating(avgRating);
    AverageRatingsChangedContext message = AverageRatingsChangedContext.create();
    message.setData(event);
    serviceApi.emit(message);
  }

}
