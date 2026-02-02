using { sap.capire.reviews as my } from '../db/schema';

@(requires: 'any')
service ReviewsService {

  /** The central entity for reviews, mostly used to add/change reviews */
  entity Reviews as projection on my.Reviews;

  /** Lightweight list of reviews without text and likes */
  @readonly entity ListOfReviews as projection on Reviews excluding { text, likes };

  /** Summary of average ratings per reviewed subject. */
  @readonly entity AverageRatings as projection on Reviews {
    key subject,
    round(avg(rating),2) as rating  : my.Rating,
    count(*)             as reviews : Integer,
  } group by subject;

  /** Event emitted when a subject's average rating has changed. */
  event AverageRatings.Changed : AverageRatings;

  /** Entities and actions for liking and unliking reviews */
  @readonly entity Likes as projection on my.Likes;
  action like (review: Review);
  action unlike (review: Review);
  type Review : projection on my.Reviews { subject, reviewer }
}
