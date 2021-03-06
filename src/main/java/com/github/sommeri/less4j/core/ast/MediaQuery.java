package com.github.sommeri.less4j.core.ast;

import java.util.ArrayList;
import java.util.List;

import com.github.sommeri.less4j.core.parser.HiddenTokenAwareTree;
import com.github.sommeri.less4j.utils.ArraysUtils;

public class MediaQuery extends ASTCssNode {

  private Medium medium;
  private List<MediaExpression> expressions;

  public MediaQuery(HiddenTokenAwareTree token) {
    this(token, null, new ArrayList<MediaExpression>());
  }

  public MediaQuery(HiddenTokenAwareTree token, Medium medium, List<MediaExpression> expressions) {
    super(token);
    this.medium = medium;
    this.expressions = expressions;
  }

  public Medium getMedium() {
    return medium;
  }

  public void setMedium(Medium medium) {
    this.medium = medium;
  }

  public List<MediaExpression> getExpressions() {
    return expressions;
  }

  public void setExpressions(List<MediaExpression> expressions) {
    this.expressions = expressions;
  }

  public void addExpression(MediaExpression expression) {
    if (expressions == null)
      expressions = new ArrayList<MediaExpression>();

    this.expressions.add(expression);
  }

  public void addExpressions(List<MediaExpression> expressions) {
    if (expressions == null)
      expressions = new ArrayList<MediaExpression>();

    this.expressions.addAll(expressions);
  }
  
  /**
   * May throw class cast exception if the member in parameter is 
   * does not have the right type. 
   */
  public void addMember(ASTCssNode member) {
    if (member.getType() == ASTCssNodeType.MEDIUM) {
      setMedium((Medium) member);
    } else {
      addExpression((MediaExpression) member);
    }
  }

  public boolean hasInterpolatedExpression() {
    for (MediaExpression mediaExpression : getExpressions()) {
      if (mediaExpression.getType()==ASTCssNodeType.INTERPOLATED_MEDIA_EXPRESSION)
        return true;
    }
    return false;
  }

  @Override
  public List<? extends ASTCssNode> getChilds() {
    List<ASTCssNode> result = ArraysUtils.asNonNullList((ASTCssNode)medium);
    result.addAll(expressions);
    return result;
  }

  @Override
  public ASTCssNodeType getType() {
    return ASTCssNodeType.MEDIA_QUERY;
  }

  @Override
  public MediaQuery clone() {
    MediaQuery result = (MediaQuery) super.clone();
    result.medium = medium==null?null:medium.clone();
    result.expressions = ArraysUtils.deeplyClonedList(expressions);
    result.configureParentToAllChilds();
    return result;
  }

}
