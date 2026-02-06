package customer.reviews;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ql.Delete;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.reviewsservice.Likes;
import cds.gen.reviewsservice.Likes_;
import cds.gen.reviewsservice.Review;
import cds.gen.reviewsservice.AverageRatingsChanged;
import cds.gen.reviewsservice.AverageRatingsChangedContext;
import cds.gen.reviewsservice.AverageRatings_;
import cds.gen.reviewsservice.ReviewsService_;
import cds.gen.reviewsservice.LikeContext;
import cds.gen.reviewsservice.Reviews;
import cds.gen.reviewsservice.ReviewsService;
import cds.gen.reviewsservice.Reviews_;
import cds.gen.reviewsservice.UnlikeContext;

@Component
@ServiceName(ReviewsService_.CDS_NAME)
public class ReviewsHandler implements EventHandler {

  @Autowired
  ReviewsService service;

  @Autowired
  PersistenceService persistenceService;

  private final Random random;

  public ReviewsHandler() {
    this.random = new Random(System.currentTimeMillis());
  }

  @On(event = { CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE } )
  public void onUpdateReview(Reviews review) {
    if(review.getRating() == null)
      review.setRating(random.nextInt(4) + 1);
  }

  @On
  public void onLike(LikeContext context) {
    Review review = context.getReview();
    String reviewer = review.getReviewer();
    String subject = review.getSubject();
    Likes like = Likes.create();
    like.setUser(context.getUserInfo().getName());
    like.setReviewReviewer(reviewer);
    like.setReviewSubject(subject);

    try {
      persistenceService.run(Insert.into(Likes_.class).entry(like));

      CqnUpdate update = Update.entity(Reviews_.class)
        .set(r -> r.liked(), liked -> liked.plus(1))
        .matching(review);
      Result updateResult = persistenceService.run(update);
      if(updateResult.rowCount() != 1)
        throw new ServiceException("Failed to update the liked counter");

    } catch(Exception e) {
      if (e.getMessage().startsWith("Unique constraint violated"))
        throw new ServiceException("You already liked that review", e);
      throw new ServiceException("Failed to perform the operation", e);
    }
    context.setCompleted();
  }

  @On
  public void onUnlike(UnlikeContext context) {
    Review review = context.getReview();
    String reviewer = review.getReviewer();
    String subject = review.getSubject();
    Likes like = Likes.create();
    like.setUser(context.getUserInfo().getName());
    like.setReviewReviewer(reviewer);
    like.setReviewSubject(subject);

    CqnDelete delete = Delete.from(Likes_.class)
      .matching(like);

    Result deleteResult = persistenceService.run(delete);
    if(deleteResult.rowCount() != 1)
      throw new ServiceException("You have not liked that review");

    CqnUpdate update = Update.entity(Reviews_.class)
      .set(r -> r.liked(), liked -> liked.minus(1))
      .matching(review);
    persistenceService.run(update);

    context.setCompleted();
  }

  @After(event = { CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE, CqnService.EVENT_DELETE } )
  public void afterChangeReview(Reviews review) {
    String subject = review.getSubject();
    var result = service.run(Select.from(AverageRatings_.class)
      .where(r -> r.subject().eq(subject)));
    var selected = result.single();
    sendReviewedMessage(subject, selected.getReviews(), selected.getRating());
  }

  private void sendReviewedMessage(String subject, Integer count, Integer avgRating) {
    AverageRatingsChanged event = AverageRatingsChanged.create();
    event.setSubject(subject);
    event.setReviews(count);
    event.setRating(avgRating);
    AverageRatingsChangedContext message = AverageRatingsChangedContext.create();
    message.setData(event);
    service.emit(message);
  }

}
