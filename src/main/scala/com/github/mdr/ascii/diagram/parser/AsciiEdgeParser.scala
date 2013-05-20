package com.github.mdr.ascii.diagram.parser

import scala.annotation.tailrec
import com.github.mdr.ascii.common.Point
import com.github.mdr.ascii.common.Direction
import com.github.mdr.ascii.common.Characters._
import com.github.mdr.ascii.common.Direction._
import scala.PartialFunction.cond

trait AsciiEdgeParser { self: DiagramParser ⇒

  @tailrec
  protected final def followAsciiEdge(points: List[Point], direction: Direction): Option[EdgeImpl] = {
    val currentPoint = points.head
    if (!inDiagram(currentPoint))
      return None
    if (isBoxEdge(currentPoint))
      return {
        if (points.size <= 2)
          None // Filter out spurious "edges" to adjacent boxes
        else
          Some(new EdgeImpl(points.reverse))
      }
    val c = charAt(currentPoint)

    val ahead = currentPoint go direction
    val left = currentPoint go direction.turnLeft
    val right = currentPoint go direction.turnRight
    val aheadIsContinuation = isContinuation(ahead, direction)
    val rightIsContinuation = isContinuation(right, direction.turnRight)
    val leftIsContinuation = isContinuation(left, direction.turnLeft)

    if (isCrossing(c) || isAheadArrow(c, direction))
      followAsciiEdge(currentPoint.go(direction) :: points, direction)
    else if (isStraightAhead(c, direction)) {
      if (aheadIsContinuation)
        followAsciiEdge(ahead :: points, direction)
      else if (leftIsContinuation && !rightIsContinuation)
        followAsciiEdge(left :: points, direction.turnLeft)
      else if (!leftIsContinuation && rightIsContinuation)
        followAsciiEdge(right :: points, direction.turnRight)
      else
        followAsciiEdge(ahead :: points, direction)
    } else if (isOrthogonal(c, direction)) {
      if (leftIsContinuation && !rightIsContinuation)
        followAsciiEdge(left :: points, direction.turnLeft)
      else if (!leftIsContinuation && rightIsContinuation)
        followAsciiEdge(right :: points, direction.turnRight)
      else
        followAsciiEdge(ahead :: points, direction)
    } else if (isLeftArrow(c, direction))
      followAsciiEdge(left :: points, direction.turnLeft)
    else if (isRightArrow(c, direction))
      followAsciiEdge(right :: points, direction.turnRight)
    else
      None
  }

  private def isContinuation(point: Point, direction: Direction): Boolean =
    isBoxEdge(point) || charAtOpt(point).exists { c ⇒
      isStraightAhead(c, direction) || isCrossing(c) || isAheadArrow(c, direction)
    }

  private def isStraightAhead(c: Char, direction: Direction): Boolean = cond(c, direction) {
    case ('-', Right | Left) ⇒ true
    case ('|', Up | Down)    ⇒ true
  }

  private def isOrthogonal(c: Char, direction: Direction): Boolean = isStraightAhead(c, direction.turnRight)

  private def isCrossing(c: Char): Boolean = c == '+'

}