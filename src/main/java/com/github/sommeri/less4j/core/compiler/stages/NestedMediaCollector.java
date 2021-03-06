package com.github.sommeri.less4j.core.compiler.stages;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import com.github.sommeri.less4j.core.ast.ASTCssNode;
import com.github.sommeri.less4j.core.ast.ASTCssNodeType;
import com.github.sommeri.less4j.core.ast.Media;
import com.github.sommeri.less4j.core.ast.MediaQuery;
import com.github.sommeri.less4j.core.problems.ProblemsHandler;
import com.github.sommeri.less4j.utils.ArraysUtils;

public class NestedMediaCollector {

  private final ASTManipulator manipulator = new ASTManipulator();
  private Stack<List<MediaQuery>> mediums;
  private LinkedList<Media> finalMedia;
  private ProblemsHandler problemsHandler;
  
  public NestedMediaCollector(ProblemsHandler problemsHandler) {
    this.problemsHandler = problemsHandler;
  }

  public List<Media> collectMedia(Media kid) {
    mediums = new Stack<List<MediaQuery>>();
    finalMedia = new LinkedList<Media>();

    pushMediums(kid);
    collectChildMedia(kid);
    popMediums();

    return finalMedia;
  }

  private void collectChildMedia(ASTCssNode node) {
    List<? extends ASTCssNode> childs = new ArrayList<ASTCssNode>(node.getChilds());
    for (ASTCssNode kid : childs) {
      if (kid.getType() == ASTCssNodeType.MEDIA) {
        Media nestedMedia = (Media) kid;
        manipulator.removeFromBody(nestedMedia);
        collect(nestedMedia);
        pushMediums(nestedMedia);
      }
      //TODO: possible optimization: there is no reason to go inside rulesets and other childs types 
      collectChildMedia(kid);
      if (kid.getType() == ASTCssNodeType.MEDIA) {
        popMediums();
      }
    }
  }

  private void collect(Media media) {
    combine(media, mediums.peek());
    finalMedia.add(media);
  }

  public void combine(Media media, List<MediaQuery> previousMediaQueries) {
    List<MediaQuery> result = new ArrayList<MediaQuery>();
    for (MediaQuery mediaQuery : media.getMediums()) {
      for (MediaQuery previousMediaQuery : previousMediaQueries) {
        result.add(combine(previousMediaQuery, mediaQuery));
      }
    }

    media.replaceMediaQueries(result);
    media.configureParentToAllChilds();
  }

  private MediaQuery combine(MediaQuery previousMediaQuery, MediaQuery mediaQuery) {
    MediaQuery previousMediaQueryClone = previousMediaQuery.clone();
    if (mediaQuery.getMedium()!=null) {
      problemsHandler.warnMerginMediaQueryWithMedium(mediaQuery);
    }
      
    previousMediaQueryClone.addExpressions(ArraysUtils.deeplyClonedList(mediaQuery.getExpressions()));
    previousMediaQueryClone.configureParentToAllChilds();
    return previousMediaQueryClone;
  }

  private void pushMediums(Media kid) {
    mediums.push(new ArrayList<MediaQuery>(kid.getMediums()));
  }

  private void popMediums() {
    mediums.pop();
  }

}
