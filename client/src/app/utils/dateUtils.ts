export function olderThenOneDay(date?: Date): boolean {
  return !date || Date.now() - date.getTime() > 24 * 60 * 60 * 1000;
}