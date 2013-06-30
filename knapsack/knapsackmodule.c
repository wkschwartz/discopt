
/*
#include "Python.h"
#include "structmember.h"
*/
#include <stdlib.h>
#include <stdio.h>

/* Input data for a knapsack problem. Used to simplify passing data into
   functions. */
typedef struct {
  size_t n;        /* problem size (length of arrays) */
  unsigned int k;  /* weight capacity */
  unsigned int *v; /* values array */
  unsigned int *w; /* weights array */
  unsigned int *x; /* decision array */
} ks_problem_t;

static ks_problem_t*
ks_problem_new(size_t n)
{
  int size = sizeof(unsigned int) * n;
  ks_problem_t *self = malloc(sizeof(ks_problem_t));
  if (self == NULL)
	return NULL;
  self->n = n;
  self->v = malloc(size);
  self->w = malloc(size);
  self->x = malloc(size);
  return self;
}

static void
ks_problem_init(ks_problem_t *self, unsigned int k, unsigned int *v,
				  unsigned int *w)
{
  int i;
  if (self == NULL) return;
  self->k = k;
  for (i = 0; i < self->n; i++) {
	self->v[i] = v[i];
	self->w[i] = w[i];
  }
}


static void
ks_problem_destroy(ks_problem_t *self)
{
  if (self == NULL) return;
  if (self->v != NULL) free(self->v);
  if (self->w != NULL) free(self->w);
  if (self->x != NULL) free(self->x);
  free(self);
}

typedef struct {
  int i;
  double ratio;
} ks_item_t;

static int
ks_compar_item(const void *first, const void *second)
{
  ks_item_t *a;
  ks_item_t *b;
  a = (ks_item_t *) first;
  b = (ks_item_t *) second;
  if (a->ratio <  b->ratio) return -1;
  if (a->ratio == b->ratio) return 0;
  return 1;
}

/**
 * Return an upper bound for the value at the best leaf of current search node.
 * See where bound() is called to understand its paramaters.
 */
static double
ks_bound(ks_problem_t *self, unsigned int i, unsigned int weight, double value,
		 ks_item_t *items)
{
  int item, wi, j;
  for (j = self->n - 1; j >= 0; j--) {
	item = items[j].i;
	if (item <= i)
	  continue;
	wi = self->w[item];
	if (wi < self->k - weight) {
	  weight += wi;                                      /* XXX overflow */
	  value += self->v[item];
	}
	else
	  return value + ((double) (self->k - weight) / wi) * self->v[item];
  }
  return value;
}

typedef struct {
  unsigned int w; /* weight */
  unsigned int v; /* value */
  unsigned int i; /* item id */
  unsigned int b; /* decision */
} ks_opt_stack_t;

static void
ks_opt_stack_push(ks_opt_stack_t *self, size_t *p, unsigned int w,
				  unsigned int v, unsigned int i, unsigned int  b)
{
  self[*p].w = w;
  self[*p].v = v;
  self[*p].i = i;
  self[*p].b = b;
  *p += 1;
}

/**
 * Fill the knapsack x as optimally as feasible.
 *
 * Errors: -1 for insufficient memory. -2 for arithmetic overflow.
 */
static long
ks_optimize (ks_problem_t *self)
{
  /* items: Items sorted by value-to-weight ratio for linear relaxation
   * stack: array of ks_opt_stack_t
   * t: the decision vector being tested at each node
   * p: stack pointer; j, weight, value: loop caches; best: max search
   * ss: stack size: Always <=2 children on stack for <=n-1 parents
   */
  ks_item_t *items;
  size_t ss = 2 * self->n, j, p = 0;
  ks_opt_stack_t *stack;
  unsigned int *t;
  unsigned int weight, value;
  long best = 0;

  /* XXX Does this protect against overflow with size_t and unsigned int? */
  if (ss < self->n || sizeof(int) * ss < ss || sizeof(int) * self->n < self->n ||
	  sizeof(ks_item_t) * self->n  < self->n)
	return -2;

  if ((items = malloc(sizeof(ks_item_t) * self->n)) == NULL)
	return -1;

  if ((t = malloc(sizeof(unsigned int) * self->n)) == NULL) {
	free(items);
	return -1;
  }
  if ((stack  = malloc(sizeof(ks_opt_stack_t) * ss)) == NULL) {
	free(items);
	free(t);
	return -1;
  }

  for (j = 0; j < self->n; j++) {
	items[j].i = j;
	items[j].ratio = (double) self->v[j] / self->w[j];
  }
  qsort(items, self->n, sizeof(ks_item_t), ks_compar_item);

  /* Push item 0 onto the stack with and without bringing it. */
  ks_opt_stack_push(stack, &p, 0, 0, 0, 1);
  ks_opt_stack_push(stack, &p, 0, 0, 0, 0);

  while (p > 0) {
	p--; /* Pop the latest item off the stack */
	weight = stack[p].w + self->w[stack[p].i] * stack[p].b;                   /* XXX overflow */
	if (weight > self->k)
	  continue;
	value = stack[p].v + self->v[stack[p].i] * stack[p].b;                    /* XXX overflow */
	if (ks_bound(self, stack[p].i, weight, value, items) < best)
	  continue;
	best = value > best ? value : best;
	t[stack[p].i] = stack[p].b;
	if (stack[p].i < self->n - 1) { // Push children onto stack w/ & w/o bringing item
	  ks_opt_stack_push(stack, &p, weight, value, stack[p].i + 1, 1);
	  ks_opt_stack_push(stack, &p, weight, value, stack[p].i + 1, 0);
	}
	else if (value >= best)
	  for (j = 0; j < self->n; j++)
		self->x[j] = t[j];
  }

  free(stack);
  free(t);
  free(items);
  return best;
}

int
main(int argc, char *argv[])
{
  int n = 4, k = 11, i, best;
  unsigned int v[] = {8, 10, 15, 4}, w[] = {4, 5, 8, 3};
  ks_problem_t *ks = ks_problem_new(n);
  if (ks == NULL) {
	ks_problem_destroy(ks);
	fprintf(stderr, "out of memory\n");
	return -1;
  }
  ks_problem_init(ks, k, v, w);
  best = ks_optimize(ks);
  printf("%d %d\n", best, 1);
  for (i = 0; i < n; i++)
	printf("%d ", ks->x[i]);
  printf("\n");
  ks_problem_destroy(ks);
  printf("test\n");
  return 0;
}
